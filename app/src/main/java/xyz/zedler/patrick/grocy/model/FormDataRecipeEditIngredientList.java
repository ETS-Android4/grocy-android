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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class FormDataRecipeEditIngredientList {

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<ArrayList<RecipePosition>> recipePositionsLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<List<QuantityUnit>> quantityUnitsLive;

  public FormDataRecipeEditIngredientList(
      Application application,
      SharedPreferences sharedPrefs,
      boolean beginnerMode
  ) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    recipePositionsLive = new MutableLiveData<>();
    productsLive = new MutableLiveData<>();
    quantityUnitsLive = new MutableLiveData<>();
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<ArrayList<RecipePosition>> getRecipePositionsLive() {
    return recipePositionsLive;
  }

  public MutableLiveData<List<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<List<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }
}
