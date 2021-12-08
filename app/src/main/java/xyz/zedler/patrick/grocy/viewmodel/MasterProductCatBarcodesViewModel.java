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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class MasterProductCatBarcodesViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatBarcodesViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final EventHandler eventHandler;
  private final MasterProductRepository repository;
  private final MasterProductFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<ProductBarcode>> productBarcodesLive;

  private ArrayList<ProductBarcode> productBarcodes;
  private ArrayList<QuantityUnit> quantityUnits;
  private ArrayList<Store> stores;
  private ArrayList<QuantityUnitConversion> unitConversions;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<Boolean> barcodeInputVisibilityLive;
  private final MutableLiveData<Boolean> amountInputVisibilityLive;
  public MutableLiveData<String> barcodeLive;
  public MutableLiveData<String> amountLive;

  public MasterProductCatBarcodesViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    eventHandler = new EventHandler();
    repository = new MasterProductRepository(application);
    args = startupArgs;

    productBarcodesLive = new MutableLiveData<>();
    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    scannerVisibilityLive = new MutableLiveData<>(false);
    barcodeInputVisibilityLive = new MutableLiveData<>(false);
    amountInputVisibilityLive = new MutableLiveData<>(false);
    barcodeLive = new MutableLiveData<>(null);
    amountLive = new MutableLiveData<>(String.valueOf(1));
  }

  public Product getFilledProduct() {
    return args.getProduct();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository
        .loadBarcodesQuantityUnitsStoresUnitConversions((barcodes, qUs, stores, conversions) -> {
          this.productBarcodes = barcodes;
          this.quantityUnits = qUs;
          this.stores = stores;
          this.unitConversions = conversions;
          productBarcodesLive.setValue(filterBarcodes(barcodes));
          if (downloadAfterLoading) {
            downloadData();
          }
        });
  }

  public MutableLiveData<Boolean> getScannerVisibilityLive() {
    return scannerVisibilityLive;
  }
  public MutableLiveData<Boolean> getBarcodeInputVisibilityLive() {
    return barcodeInputVisibilityLive;
  }
  public MutableLiveData<Boolean> getAmountInputVisibilityLive() {
    return amountInputVisibilityLive;
  }

  public void uploadBarcode(ProductBarcode barcode) {
    JSONObject jsonObject = barcode.getJsonFromProductBarcode(debug, TAG);
    dlHelper.post(
        grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
        jsonObject,
        response -> {
          int objectId = -1;
          try {
            objectId = response.getInt("created_object_id");
            Log.i(TAG, "uploadBarcode: " + objectId);
          } catch (JSONException e) {
            if (debug) {
              Log.e(TAG, "uploadBarcode: " + e);
            }
          }
          if (objectId != -1) {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, objectId);
            sendEvent(Event.SET_PRODUCT_ID, bundle);
          }
          sendEvent(Event.NAVIGATE_UP);
        },
        error -> {
          showErrorMessage();
          if (debug) {
            Log.e(TAG, "uploadBarcode: " + error);
          }
        }
    );
  }


  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateProductBarcodes(
            dbChangedTime,
            barcodes -> this.productBarcodes = barcodes
        ), dlHelper.updateQuantityUnits(
            dbChangedTime,
            qUs -> this.quantityUnits = qUs
        ), dlHelper.updateStores(
            dbChangedTime,
            stores -> this.stores = stores
        ), dlHelper.updateQuantityUnitConversions(
            dbChangedTime,
            conversions -> this.unitConversions = conversions
        )
    );
    if (queue.isEmpty()) {
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    productBarcodesLive.setValue(filterBarcodes(productBarcodes));
    repository
        .updateBarcodesQuantityUnitsStoresUnitConversions(productBarcodes, quantityUnits, stores,
            unitConversions, () -> {
            });
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  private ArrayList<ProductBarcode> filterBarcodes(ArrayList<ProductBarcode> barcodes) {
    ArrayList<ProductBarcode> filteredBarcodes = new ArrayList<>();
    assert args.getProduct() != null;
    int productId = args.getProduct().getId();
    for (ProductBarcode barcode : barcodes) {
      if (barcode.getProductId() == productId) {
        filteredBarcodes.add(barcode);
      }
    }
    return filteredBarcodes;
  }

  public MutableLiveData<ArrayList<ProductBarcode>> getProductBarcodesLive() {
    return productBarcodesLive;
  }

  public ArrayList<QuantityUnit> getQuantityUnits() {
    return quantityUnits;
  }

  public ArrayList<Store> getStores() {
    return stores;
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public void showErrorMessage() {
    showMessage(getString(R.string.error_undefined));
  }

  @NonNull
  public EventHandler getEventHandler() {
    return eventHandler;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MasterProductCatBarcodesViewModelFactory implements
      ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductFragmentArgs args;

    public MasterProductCatBarcodesViewModelFactory(
        Application application,
        MasterProductFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductCatBarcodesViewModel(application, args);
    }
  }
}
