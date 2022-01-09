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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.os.AsyncTask;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Store;

public class MasterProductRepository {

  private final AppDatabase appDatabase;

  public MasterProductRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {

    void actionFinished(
        ArrayList<Product> products,
        ArrayList<ProductGroup> productGroups,
        ArrayList<ProductBarcode> barcodes
    );
  }

  public interface ProductsListener {

    void actionFinished(ArrayList<Product> products);
  }

  public interface LocationsStoresListener {

    void actionFinished(ArrayList<Location> locations, ArrayList<Store> stores);
  }

  public interface QuantityUnitsListener {

    void actionFinished(ArrayList<QuantityUnit> quantityUnits);
  }

  public interface BarcodesQuantityUnitsStoresUnitConversionsListener {

    void actionFinished(ArrayList<ProductBarcode> barcodes, ArrayList<QuantityUnit> quantityUnits,
        ArrayList<Store> stores, ArrayList<QuantityUnitConversion> conversions);
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListener listener) {
    new loadAsyncTask(appDatabase, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataListener listener;

    private ArrayList<Product> products;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<ProductBarcode> barcodes;

    loadAsyncTask(AppDatabase appDatabase, DataListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      products = new ArrayList<>(appDatabase.productDao().getAll());
      productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
      barcodes = new ArrayList<>(appDatabase.productBarcodeDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(products, productGroups, barcodes);
      }
    }
  }

  public void updateDatabase(
      ArrayList<Product> products,
      ArrayList<ProductGroup> productGroups,
      ArrayList<ProductBarcode> barcodes,
      DataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        products,
        productGroups,
        barcodes,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<Product> products;
    private final ArrayList<ProductGroup> productGroups;
    private final ArrayList<ProductBarcode> barcodes;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<Product> products,
        ArrayList<ProductGroup> productGroups,
        ArrayList<ProductBarcode> barcodes,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.products = products;
      this.productGroups = productGroups;
      this.barcodes = barcodes;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      appDatabase.productGroupDao().deleteAll();
      appDatabase.productGroupDao().insertAll(productGroups);
      appDatabase.productBarcodeDao().deleteAll();
      appDatabase.productBarcodeDao().insertAll(barcodes);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }

  public void loadProductsFromDatabase(ProductsListener listener) {
    new loadProductsAsyncTask(appDatabase, listener).execute();
  }

  private static class loadProductsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final ProductsListener listener;

    private ArrayList<Product> products;

    loadProductsAsyncTask(AppDatabase appDatabase, ProductsListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      products = new ArrayList<>(appDatabase.productDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(products);
      }
    }
  }

  public void updateDatabase(
      ArrayList<Product> products,
      DataUpdatedListener listener
  ) {
    new updateProductsAsyncTask(appDatabase, products, listener).execute();
  }

  private static class updateProductsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<Product> products;

    updateProductsAsyncTask(
        AppDatabase appDatabase,
        ArrayList<Product> products,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.products = products;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }

  public void loadLocationsStoresFromDatabase(LocationsStoresListener listener) {
    new loadLocationsStoresAsyncTask(appDatabase, listener).execute();
  }

  private static class loadLocationsStoresAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final LocationsStoresListener listener;

    private ArrayList<Location> locations;
    private ArrayList<Store> stores;

    loadLocationsStoresAsyncTask(AppDatabase appDatabase, LocationsStoresListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      locations = new ArrayList<>(appDatabase.locationDao().getAll());
      stores = new ArrayList<>(appDatabase.storeDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(locations, stores);
      }
    }
  }

  public void updateLocationsStoresDatabase(
      ArrayList<Location> locations,
      ArrayList<Store> stores,
      DataUpdatedListener listener
  ) {
    new updateLocationsStoresAsyncTask(
        appDatabase,
        locations,
        stores,
        listener
    ).execute();
  }

  private static class updateLocationsStoresAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<Location> locations;
    private final ArrayList<Store> stores;

    updateLocationsStoresAsyncTask(
        AppDatabase appDatabase,
        ArrayList<Location> locations,
        ArrayList<Store> stores,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.locations = locations;
      this.stores = stores;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.locationDao().deleteAll();
      appDatabase.locationDao().insertAll(locations);
      appDatabase.storeDao().deleteAll();
      appDatabase.storeDao().insertAll(stores);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }

  public void loadQuantityUnitsFromDatabase(QuantityUnitsListener listener) {
    new loadQuantityUnitsAsyncTask(appDatabase, listener).execute();
  }

  private static class loadQuantityUnitsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final QuantityUnitsListener listener;

    private ArrayList<QuantityUnit> quantityUnits;

    loadQuantityUnitsAsyncTask(AppDatabase appDatabase, QuantityUnitsListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(quantityUnits);
      }
    }
  }

  public void updateQuantityUnitsDatabase(
      ArrayList<QuantityUnit> quantityUnits,
      DataUpdatedListener listener
  ) {
    new updateQuantityUnitsAsyncTask(
        appDatabase,
        quantityUnits,
        listener
    ).execute();
  }

  private static class updateQuantityUnitsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<QuantityUnit> quantityUnits;

    updateQuantityUnitsAsyncTask(
        AppDatabase appDatabase,
        ArrayList<QuantityUnit> quantityUnits,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.quantityUnits = quantityUnits;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.locationDao().deleteAll();
      appDatabase.quantityUnitDao().insertAll(quantityUnits);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }

  public void loadBarcodesQuantityUnitsStoresUnitConversions(
      BarcodesQuantityUnitsStoresUnitConversionsListener listener) {
    new loadBarcodesQuantityUnitsStoresUnitConversionsAsyncTask(appDatabase, listener).execute();
  }

  private static class loadBarcodesQuantityUnitsStoresUnitConversionsAsyncTask extends
      AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final BarcodesQuantityUnitsStoresUnitConversionsListener listener;

    private ArrayList<ProductBarcode> barcodes;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Store> stores;
    private ArrayList<QuantityUnitConversion> conversions;

    loadBarcodesQuantityUnitsStoresUnitConversionsAsyncTask(AppDatabase appDatabase,
        BarcodesQuantityUnitsStoresUnitConversionsListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      barcodes = new ArrayList<>(appDatabase.productBarcodeDao().getAll());
      quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
      stores = new ArrayList<>(appDatabase.storeDao().getAll());
      conversions = new ArrayList<>(appDatabase.quantityUnitConversionDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(barcodes, quantityUnits, stores, conversions);
      }
    }
  }

  public void updateBarcodesQuantityUnitsStoresUnitConversions(
      ArrayList<ProductBarcode> barcodes,
      ArrayList<QuantityUnit> quantityUnits,
      ArrayList<Store> stores,
      ArrayList<QuantityUnitConversion> conversions,
      DataUpdatedListener listener
  ) {
    new updateBarcodesQuantityUnitsStoresUnitConversionsAsyncTask(
        appDatabase,
        barcodes,
        quantityUnits,
        stores,
        conversions,
        listener
    ).execute();
  }

  private static class updateBarcodesQuantityUnitsStoresUnitConversionsAsyncTask extends
      AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<ProductBarcode> barcodes;
    private final ArrayList<QuantityUnit> quantityUnits;
    private final ArrayList<Store> stores;
    private final ArrayList<QuantityUnitConversion> conversions;

    updateBarcodesQuantityUnitsStoresUnitConversionsAsyncTask(
        AppDatabase appDatabase,
        ArrayList<ProductBarcode> barcodes,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<Store> stores,
        ArrayList<QuantityUnitConversion> conversions,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.barcodes = barcodes;
      this.quantityUnits = quantityUnits;
      this.stores = stores;
      this.conversions = conversions;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.productBarcodeDao().deleteAll();
      appDatabase.productBarcodeDao().insertAll(barcodes);
      appDatabase.quantityUnitDao().deleteAll();
      appDatabase.quantityUnitDao().insertAll(quantityUnits);
      appDatabase.storeDao().deleteAll();
      appDatabase.storeDao().insertAll(stores);
      appDatabase.quantityUnitConversionDao().deleteAll();
      appDatabase.quantityUnitConversionDao().insertAll(conversions);
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
