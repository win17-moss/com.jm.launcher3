package com.jm.launcher3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jm.launcher3.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UserActivity extends AppCompatActivity {

    private EditText editText;
    private Button saveButton;
    private Button cancelButton;
    private String filePath;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // 显示弹窗
        showUpdateDialog();
    }

    private void showUpdateDialog() {
        // 获取布局文件并初始化
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_user, null);

        // 创建AlertDialog.Builder实例
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // 初始化控件
        editText = dialogView.findViewById(R.id.editText);
        saveButton = dialogView.findViewById(R.id.saveButton);
        cancelButton = dialogView.findViewById(R.id.cancelButton);

        // 设置文件路径
        filePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/userinf/user.xml";

        // 加载文件内容
        loadFileContent();

        // 保存按钮点击事件
        saveButton.setOnClickListener(v -> {
            if (saveFileContent()) {
                dialog.dismiss(); // 保存成功后关闭弹窗
            }
        });

        // 取消按钮点击事件
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // 创建并显示弹窗
        dialog = builder.create();
        dialog.show();
    }

    private boolean saveFileContent() {
        String content = editText.getText().toString();
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // 确保父目录存在

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
            return true; // 保存成功
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "修改失败，请稍后重试", Toast.LENGTH_SHORT).show();
            return false; // 保存失败
        }
    }

    private void loadFileContent() {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[fis.available()];
                fis.read(data);
                String content = new String(data, StandardCharsets.UTF_8);
                editText.setText(content);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}