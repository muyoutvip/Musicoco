package com.duan.musicoco.app;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.duan.musicoco.R;
import com.duan.musicoco.app.interfaces.ThemeChangeable;
import com.duan.musicoco.preference.ThemeEnum;
import com.duan.musicoco.util.ColorUtils;

public class MeActivity extends RootActivity implements ThemeChangeable {

    private Toolbar toolbar;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);

        initViews();
        themeChange(null, null);
        initData();
    }

    private void initData() {

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

    }

    private void initViews() {

        toolbar = (Toolbar) findViewById(R.id.me_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.me_recycle_view);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void themeChange(ThemeEnum themeEnum, int[] colors) {

        int[] cs2 = ColorUtils.get2ActionStatusBarColors(this);
        int actionC = cs2[0];
        int statusC = cs2[1];
        toolbar.setBackgroundColor(statusC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(actionC);
        }
    }

}