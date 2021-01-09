package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.ListItem;

public class MasterProductGroupBottomSheet extends BaseBottomSheet {

	private final static String TAG = MasterProductGroupBottomSheet.class.getSimpleName();

	private MainActivity activity;
	private ProductGroup productGroup;
	private ListItem itemName, itemDescription;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_master_product_group,
				container,
				false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		Bundle bundle = getArguments();
		if(bundle != null) productGroup = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_GROUP);

		// VIEWS

		itemName = view.findViewById(R.id.item_master_product_group_name);
		itemDescription = view.findViewById(R.id.item_master_product_group_description);

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_master_product_group);
		toolbar.setOnMenuItemClickListener(item -> {
			BaseFragment fragmentCurrent = activity.getCurrentFragment();
			if(item.getItemId() == R.id.action_edit) {
				fragmentCurrent.editObject(productGroup);
			} else if(item.getItemId() == R.id.action_delete) {
				fragmentCurrent.deleteObjectSafely(productGroup);
			}
			dismiss();
			return true;
		});

		setData();

		return view;
	}

	private void setData() {
		// NAME
		itemName.setText(activity.getString(R.string.property_name), productGroup.getName());

		// DESCRIPTION
		String description = productGroup.getDescription();
		if(description != null && !description.isEmpty()) {
			itemDescription.setSingleLine(false);
			itemDescription.setText(activity.getString(R.string.property_description), description);
		} else {
			itemDescription.setVisibility(View.GONE);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
