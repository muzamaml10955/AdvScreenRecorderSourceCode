package com.coolapps.screenrecorder.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.coolapps.screenrecorder.Const;
import com.coolapps.screenrecorder.R;
import com.coolapps.screenrecorder.adapter.Apps;
import com.coolapps.screenrecorder.adapter.AppsListFragmentAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AppPickerPreference extends DialogPreference implements AppsListFragmentAdapter.OnItemClicked {
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ArrayList<Apps> apps;

    public AppPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);

        setDialogLayoutResource(R.layout.layout_apps_list_preference);

    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(null, null);
    }

    @Override
    protected View onCreateDialogView() {
        return super.onCreateDialogView();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        progressBar = view.findViewById(R.id.appsProgressBar);
        recyclerView = view.findViewById(R.id.appsRecyclerView);

        init();
    }

    private void init() {
        RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        new GetApps().execute();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }

    @Override
    public void onItemClick(int position) {
        Log.d(Const.TAG, "Closing dialog. received result. Pos:" + position);
        persistString(apps.get(position).getPackageName());
        getDialog().dismiss();
    }

    class GetApps extends AsyncTask<Void, Void, ArrayList<Apps>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ArrayList<Apps> apps) {
            super.onPostExecute(apps);

            progressBar.setVisibility(View.GONE);
            AppsListFragmentAdapter recyclerViewAdapter = new AppsListFragmentAdapter(apps);

            recyclerView.setAdapter(recyclerViewAdapter);

            recyclerViewAdapter.setOnClick(AppPickerPreference.this);
        }

        @Override
        protected ArrayList<Apps> doInBackground(Void... voids) {
            PackageManager pm = getContext().getPackageManager();
            apps = new ArrayList<>();

            List<PackageInfo> packages = pm.getInstalledPackages(0);

            for (PackageInfo packageInfo : packages) {

                if (!(getContext().getPackageName().equals(packageInfo.packageName))
                        && !(pm.getLaunchIntentForPackage(packageInfo.packageName) == null)) {

                    Apps app = new Apps(
                            packageInfo.applicationInfo.loadLabel(getContext().getPackageManager()).toString(),
                            packageInfo.packageName,
                            packageInfo.applicationInfo.loadIcon(getContext().getPackageManager())

                    );

                    app.setSelectedApp(
                            AppPickerPreference.this.getPersistedString("none")
                                    .equals(packageInfo.packageName)
                    );
                    if (pm.getLaunchIntentForPackage(packageInfo.packageName) == null)
                        Log.d(Const.TAG, packageInfo.packageName);
                    apps.add(app);
                }
                Collections.sort(apps);
            }
            return apps;
        }
    }
}
