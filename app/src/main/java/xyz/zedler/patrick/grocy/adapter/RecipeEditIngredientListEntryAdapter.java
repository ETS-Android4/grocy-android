/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowRecipeEditListEntryBinding;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class RecipeEditIngredientListEntryAdapter extends
    RecyclerView.Adapter<RecipeEditIngredientListEntryAdapter.ViewHolder> {

  private final static String TAG = RecipeEditIngredientListEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<RecipePosition> recipePositions;
  private final ArrayList<Product> products;
  private final ArrayList<QuantityUnit> quantityUnits;
  private final RecipeEditIngredientListEntryAdapterListener listener;

  private final PluralUtil pluralUtil;

  public RecipeEditIngredientListEntryAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<RecipePosition> recipePositions,
      ArrayList<Product> products,
      ArrayList<QuantityUnit> quantityUnits,
      RecipeEditIngredientListEntryAdapterListener listener
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.recipePositions = new ArrayList<>(recipePositions);
    this.products = new ArrayList<>(products);
    this.quantityUnits = new ArrayList<>(quantityUnits);
    this.listener = listener;
    this.pluralUtil = new PluralUtil(context);
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class RecipeEditIngredientListEntryViewHolder extends ViewHolder {

    private final RowRecipeEditListEntryBinding binding;

    public RecipeEditIngredientListEntryViewHolder(RowRecipeEditListEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new RecipeEditIngredientListEntryViewHolder(RowRecipeEditListEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();
    RecipeEditIngredientListEntryViewHolder holder = (RecipeEditIngredientListEntryViewHolder) viewHolder;

    RecipePosition recipePosition = recipePositions.get(position);
    Product product = Product.getProductFromId(products, recipePosition.getProductId());
    QuantityUnit quantityUnit = QuantityUnit.getFromId(quantityUnits, recipePosition.getQuantityUnitId());

    if (product == null || quantityUnit == null)
      return;

    holder.binding.title.setText(product.getName());

    // AMOUNT
    holder.binding.quantity.setText(
            context.getString(
                    R.string.subtitle_amount,
                    NumUtil.trim(recipePosition.getAmount()),
                    pluralUtil.getQuantityUnitPlural(quantityUnit, recipePosition.getAmount())
            )
    );

    // CONTAINER

    holder.binding.linearRecipeIngredientContainer.setOnClickListener(
            view -> listener.onItemRowClicked(recipePosition, position)
    );
  }

  @Override
  public int getItemCount() {
    return recipePositions.size();
  }

  public interface RecipeEditIngredientListEntryAdapterListener {

    void onItemRowClicked(RecipePosition recipePosition, int position);
  }

  public void updateData(
      ArrayList<RecipePosition> newList,
      ArrayList<Product> newProducts,
      ArrayList<QuantityUnit> newQuantityUnits
  ) {

    RecipeEditIngredientListEntryAdapter.DiffCallback diffCallback = new RecipeEditIngredientListEntryAdapter.DiffCallback(
        this.recipePositions,
        newList,
        this.products,
        newProducts,
        this.quantityUnits,
        newQuantityUnits
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.recipePositions.clear();
    this.recipePositions.addAll(newList);
    this.products.clear();
    this.products.addAll(newProducts);
    this.quantityUnits.clear();
    this.quantityUnits.addAll(newQuantityUnits);
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<RecipePosition> oldItems;
    ArrayList<RecipePosition> newItems;
    ArrayList<Product> oldProducts;
    ArrayList<Product> newProducts;
    ArrayList<QuantityUnit> oldQuantityUnits;
    ArrayList<QuantityUnit> newQuantityUnits;

    public DiffCallback(
        ArrayList<RecipePosition> oldItems,
        ArrayList<RecipePosition> newItems,
        ArrayList<Product> oldProducts,
        ArrayList<Product> newProducts,
        ArrayList<QuantityUnit> oldQuantityUnits,
        ArrayList<QuantityUnit> newQuantityUnits
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldProducts = oldProducts;
      this.newProducts = newProducts;
      this.oldQuantityUnits = oldQuantityUnits;
      this.newQuantityUnits = newQuantityUnits;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      RecipePosition newItem = newItems.get(newItemPos);
      RecipePosition oldItem = oldItems.get(oldItemPos);
      Product newItemProduct = Product.getProductFromId(newProducts, newItem.getProductId());
      Product oldItemProduct = Product.getProductFromId(oldProducts, oldItem.getProductId());
      QuantityUnit newQuantityUnit = QuantityUnit.getFromId(newQuantityUnits, newItem.getQuantityUnitId());
      QuantityUnit oldQuantityUnit = QuantityUnit.getFromId(oldQuantityUnits, oldItem.getQuantityUnitId());

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      if (newItemProduct == null || oldItemProduct == null || !newItemProduct.equals(oldItemProduct)) {
        return false;
      }

      if (newQuantityUnit == null || oldQuantityUnit == null || !newQuantityUnit.equals(oldQuantityUnit)) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final RecipeEditIngredientListEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull RecipeEditIngredientListEntryAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
