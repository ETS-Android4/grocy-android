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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

public class ShoppingListRepository {

  private final AppDatabase appDatabase;

  public ShoppingListRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface ShoppingListDataListener {

    void actionFinished(
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingList> shoppingLists,
        ArrayList<ProductGroup> productGroups,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<QuantityUnitConversion> unitConversions,
        ArrayList<Product> products,
        ArrayList<Store> stores,
        ArrayList<MissingItem> missingItems
    );
  }

  public interface ShoppingListDataUpdatedListener {

    void actionFinished(
        ArrayList<ShoppingListItem> offlineChangedItems,
        HashMap<Integer, ShoppingListItem> serverItemsHashMap
    );
  }

  public interface ShoppingListItemsInsertedListener {

    void actionFinished();
  }

  public void loadFromDatabase(ShoppingListDataListener listener) {
    new loadAsyncTask(appDatabase, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final ShoppingListDataListener listener;

    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnitConversion> unitConversions;
    private ArrayList<Product> products;
    private ArrayList<Store> stores;
    private ArrayList<MissingItem> missingItems;

    loadAsyncTask(AppDatabase appDatabase, ShoppingListDataListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      shoppingListItems = new ArrayList<>(
          appDatabase.shoppingListItemDao().getAll()); // TODO: List instead of ArrayList maybe
      shoppingLists = new ArrayList<>(appDatabase.shoppingListDao().getAll());
      productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
      quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
      unitConversions = new ArrayList<>(appDatabase.quantityUnitConversionDao().getAll());
      products = new ArrayList<>(appDatabase.productDao().getAll());
      stores = new ArrayList<>(appDatabase.storeDao().getAll());
      missingItems = new ArrayList<>(appDatabase.missingItemDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(shoppingListItems, shoppingLists, productGroups, quantityUnits,
            unitConversions, products, stores, missingItems);
      }
    }
  }

  public void updateDatabase(
      ArrayList<ShoppingListItem> shoppingListItems,
      ArrayList<ShoppingList> shoppingLists,
      ArrayList<ProductGroup> productGroups,
      ArrayList<QuantityUnit> quantityUnits,
      ArrayList<QuantityUnitConversion> unitConversions,
      ArrayList<Product> products,
      ArrayList<Store> stores,
      ArrayList<MissingItem> missingItems,
      ShoppingListDataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        shoppingListItems,
        shoppingLists,
        productGroups,
        quantityUnits,
        unitConversions,
        products,
        stores,
        missingItems,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final ShoppingListDataUpdatedListener listener;

    private final ArrayList<ShoppingListItem> shoppingListItems;
    private final ArrayList<ShoppingList> shoppingLists;
    private final ArrayList<ProductGroup> productGroups;
    private final ArrayList<QuantityUnit> quantityUnits;
    private final ArrayList<QuantityUnitConversion> unitConversions;
    private final ArrayList<Product> products;
    private final ArrayList<Store> stores;
    private final ArrayList<MissingItem> missingItems;
    private final ArrayList<ShoppingListItem> itemsToSync;
    private final HashMap<Integer, ShoppingListItem> serverItemsHashMap;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingList> shoppingLists,
        ArrayList<ProductGroup> productGroups,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<QuantityUnitConversion> unitConversions,
        ArrayList<Product> products,
        ArrayList<Store> stores,
        ArrayList<MissingItem> missingItems,
        ShoppingListDataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.shoppingListItems = shoppingListItems;
      this.shoppingLists = shoppingLists;
      this.productGroups = productGroups;
      this.quantityUnits = quantityUnits;
      this.unitConversions = unitConversions;
      this.products = products;
      this.stores = stores;
      this.missingItems = missingItems;
      this.itemsToSync = new ArrayList<>();
      this.serverItemsHashMap = new HashMap<>();
    }

    @Override
    protected final Void doInBackground(Void... params) {
      for (ShoppingListItem s : shoppingListItems) {
        serverItemsHashMap.put(s.getId(), s);
      }

      List<ShoppingListItem> offlineItems = appDatabase.shoppingListItemDao().getAll();
      // compare server items with offline items and add modified to separate list
      for (ShoppingListItem offlineItem : offlineItems) {
        ShoppingListItem serverItem = serverItemsHashMap.get(offlineItem.getId());
        if (serverItem != null  // sync only items which are still on server
            && offlineItem.getDoneSynced() != -1
            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
            && offlineItem.getDoneInt() != serverItem.getDoneInt()
            || serverItem != null
            && serverItem.getDoneSynced() != -1  // server database hasn't changed
            && offlineItem.getDoneSynced() != -1
            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
        ) {
          itemsToSync.add(offlineItem);
        }
      }

      appDatabase.shoppingListItemDao().deleteAll();
      appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
      appDatabase.shoppingListDao().deleteAll();
      appDatabase.shoppingListDao().insertAll(shoppingLists);
      appDatabase.productGroupDao().deleteAll();
      appDatabase.productGroupDao().insertAll(productGroups);
      appDatabase.quantityUnitDao().deleteAll();
      appDatabase.quantityUnitDao().insertAll(quantityUnits);
      appDatabase.quantityUnitConversionDao().deleteAll();
      appDatabase.quantityUnitConversionDao().insertAll(unitConversions);
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      appDatabase.storeDao().deleteAll();
      appDatabase.storeDao().insertAll(stores);
      appDatabase.missingItemDao().deleteAll();
      appDatabase.missingItemDao().insertAll(missingItems);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(itemsToSync, serverItemsHashMap);
      }
    }
  }

  public void insertShoppingListItems(
      ShoppingListItemsInsertedListener listener,
      ShoppingListItem... shoppingListItems
  ) {
    new insertShoppingListItemsAsyncTask(appDatabase, listener).execute(shoppingListItems);
  }

  private static class insertShoppingListItemsAsyncTask extends
      AsyncTask<ShoppingListItem, Void, Void> {

    private final AppDatabase appDatabase;
    private final ShoppingListItemsInsertedListener listener;

    insertShoppingListItemsAsyncTask(
        AppDatabase appDatabase,
        ShoppingListItemsInsertedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(ShoppingListItem... items) {
      appDatabase.shoppingListItemDao().insertAll(items);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }

  public interface ShoppingListsListener {

    void actionFinished(ArrayList<ShoppingList> shoppingLists);
  }

  public void loadShoppingListsFromDatabase(ShoppingListsListener listener) {
    new loadShoppingListsAsyncTask(appDatabase, listener).execute();
  }

  private static class loadShoppingListsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final ShoppingListsListener listener;

    private ArrayList<ShoppingList> shoppingLists;

    loadShoppingListsAsyncTask(AppDatabase appDatabase, ShoppingListsListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      shoppingLists = new ArrayList<>(appDatabase.shoppingListDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(shoppingLists);
      }
    }
  }

  public LiveData<List<ShoppingList>> getShoppingListsLive() {
    return appDatabase.shoppingListDao().getAllLive();
  }

  public interface ShoppingListsUpdatedListener {

    void actionFinished();
  }

  public void updateDatabase(
      ArrayList<ShoppingList> shoppingLists,
      ShoppingListsUpdatedListener listener
  ) {
    new updateShoppingListsAsyncTask(
        appDatabase,
        shoppingLists,
        listener
    ).execute();
  }

  private static class updateShoppingListsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final ShoppingListsUpdatedListener listener;

    private final ArrayList<ShoppingList> shoppingLists;

    updateShoppingListsAsyncTask(
        AppDatabase appDatabase,
        ArrayList<ShoppingList> shoppingLists,
        ShoppingListsUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.shoppingLists = shoppingLists;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.shoppingListDao().deleteAll();
      appDatabase.shoppingListDao().insertAll(shoppingLists);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }
}
