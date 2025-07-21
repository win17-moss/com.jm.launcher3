package com.jm.launcher3;


import android.os.Bundle;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;



public class pullnew extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            Toast.makeText(this, "目标应用未安装", Toast.LENGTH_SHORT).show();


        // 结束当前活动
        finish();
    }
}