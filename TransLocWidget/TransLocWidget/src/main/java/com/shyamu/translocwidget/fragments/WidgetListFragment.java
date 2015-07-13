package com.shyamu.translocwidget.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.shyamu.translocwidget.bl.ArrivalTimeWidget;
import com.shyamu.translocwidget.listview.ListViewAdapter;

import com.shyamu.translocwidget.bl.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class WidgetListFragment extends ListFragment {

    private static final String TAG = "WidgetListFragment";
    private OnFragmentInteractionListener mListener;

    public static WidgetListFragment newInstance() {
        WidgetListFragment fragment = new WidgetListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public WidgetListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListViewAdapter widgetListViewAdapter = new ListViewAdapter(getActivity());
        ArrayList<ArrivalTimeWidget> listViewArray = null;
        try {
            listViewArray = Utils.getArrivalTimeWidgetsFromStorage(getActivity());
        } catch (IOException e) {
            try {
                Utils.writeArrivalTimeWidgetsToStorage(getActivity(), new ArrayList<>());
            } catch (IOException e1) {
                Log.e(TAG, "Error in writing empty widget list", e1);
            }
            Log.e(TAG, "Error in getting previous widget list", e);
        }
        if(listViewArray != null) {
            if(listViewArray.isEmpty()) {

            }
            widgetListViewAdapter.setWidgetList(listViewArray);
        }
        setListAdapter(widgetListViewAdapter);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            ArrivalTimeWidget widget = (ArrivalTimeWidget) l.getItemAtPosition(position);
            if(widget == null) throw new IllegalStateException();
            else {
                Log.d(TAG, widget.toString());
                mListener.onFragmentInteraction(widget);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(ArrivalTimeWidget widget);
    }

}
