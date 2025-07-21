package com.jm.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.jm.launcher3.AppAdapter;
import com.jm.launcher3.R;
import com.jm.launcher3.ToolsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private GridView appGridView;
    private TextView fileContentTextView;
    private List<ApplicationInfo> appList;
    private List<String> hiddenAppList; // 用于存储不显示的应用包名
    private List<String> manualOrder; // 用于存储手动排序的应用包名
    private String filePath;
    private String packFilePath;
    private String manualOrderFilePath;
    private AlertDialog dialog; // 定义 dialog 为成员变量

    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appGridView = findViewById(R.id.app_grid);
        fileContentTextView = findViewById(R.id.fileContentTextView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(0x000000);
        }
        setDefaultLauncher();

        // 设置透明状态栏
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0x000000); // 设置状态栏颜色为透明

        // 初始化文件路径
        filePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/userinf/user.xml";
        packFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/userinf/pack.xml";
        manualOrderFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/userinf/manual_order.xml";

        // 初始化应用列表
        refreshAppList();

        // 设置适配器
        AppAdapter adapter = new AppAdapter(this, appList, getPackageManager());
        appGridView.setAdapter(adapter);

        // 设置点击事件
        appGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo app = appList.get(position);
                Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (intent != null) {
                    // 检查包名是否为 com.example.cc
                    if ("com.jm.cc".equals(app.packageName)) {
                        refreshAppList();
                        AppAdapter adapter = new AppAdapter(MainActivity.this, appList, getPackageManager());
                        appGridView.setAdapter(adapter);
                        fetchAndSaveHiddenAppList();
                        fetchAndSaveManualOrder();
//                        Toast.makeText(MainActivity.this, "拉取最新策略成功", Toast.LENGTH_SHORT).show();
                    }
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "无法打开该应用", Toast.LENGTH_SHORT).show();
                }
            }
        });



        ImageButton button = findViewById(R.id.menu_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建 PopupMenu
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
                MenuInflater menuInflater = getMenuInflater();
                menuInflater.inflate(R.menu.menu_main, popupMenu.getMenu());

                // 设置菜单项点击事件
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.menu_privacy_policy) {
                            // 跳转到 PrivacyPolicyActivity
                            Intent intentPrivacy = new Intent(MainActivity.this, ToolsActivity.class);
                            startActivity(intentPrivacy);
                            return true;
                        } else if (itemId == R.id.menu_account_settings) {
                            // 显示 UserActivity 弹窗
                            showUserDialog();
                            return true;
                        }
                        return false;
                    }
                });

                // 显示 PopupMenu
                popupMenu.show();
            }
        });

        // 加载文件内容
        loadFileContent();
    }

    private void setDefaultLauncher() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFileContent(); // 每次回到 MainActivity 时重新加载文件内容
        refreshAppList();  // 每次回到 MainActivity 时刷新应用列表
        AppAdapter adapter = new AppAdapter(this, appList, getPackageManager());
        appGridView.setAdapter(adapter);
//        startRefreshTask();
    }

//    private void startRefreshTask() {
//        refreshRunnable = new Runnable() {
//            @Override
//            public void run() {
//                refreshAppList();
//                AppAdapter adapter = new AppAdapter(MainActivity.this, appList, getPackageManager());
//                appGridView.setAdapter(adapter);
//                handler.postDelayed(this, 5000); // 每5秒刷新一次
//            }
//        };
//        handler.postDelayed(refreshRunnable, 5000); // 初始延迟5秒后开始执行
//    }

    private void fetchAndSaveHiddenAppList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://162.14.113.207:8898/api/unapp/list.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // 解析 JSON 数据
                        hiddenAppList = new ArrayList<>();
                        JSONArray jsonArray = new JSONArray(response.toString());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String packageName = jsonObject.getString("id");
                            hiddenAppList.add(packageName);
                        }

                        // 保存到 pack.xml 文件
                        saveHiddenAppListToFile(hiddenAppList, packFilePath);

                        // 刷新应用列表
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshAppList();
                                AppAdapter adapter = new AppAdapter(MainActivity.this, appList, getPackageManager());
                                appGridView.setAdapter(adapter);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "拉取最新策略失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "策略更新失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void fetchAndSaveManualOrder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://162.14.113.207:8898/api/unapp/stupapp.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // 解析 JSON 数据
                        manualOrder = new ArrayList<>();
                        JSONArray jsonArray = new JSONArray(response.toString());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String packageName = jsonArray.getString(i);
                            manualOrder.add(packageName);
                        }

                        // 保存到 manual_order.xml 文件
                        saveManualOrderToFile(manualOrder, manualOrderFilePath);

                        // 刷新应用列表
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshAppList();
                                AppAdapter adapter = new AppAdapter(MainActivity.this, appList, getPackageManager());
                                appGridView.setAdapter(adapter);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "策略更新失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "策略更新失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void saveHiddenAppListToFile(List<String> hiddenAppList, String filePath) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("<hidden-apps>\n");
            for (String packageName : hiddenAppList) {
                writer.write("    <app>" + packageName + "</app>\n");
            }
            writer.write("</hidden-apps>");
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "策略更新失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private List<String> loadHiddenAppListFromFile(String filePath) {
        List<String> hiddenAppList = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("<app>")) {
                        String packageName = line.trim().substring(5, line.trim().length() - 6);
                        hiddenAppList.add(packageName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "策略更新失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return hiddenAppList;
    }

    private void saveManualOrderToFile(List<String> manualOrder, String filePath) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("<manual-order>\n");
            for (String packageName : manualOrder) {
                writer.write("    <app>" + packageName + "</app>\n");
            }
            writer.write("</manual-order>");
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "手动排序列表更新失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private List<String> loadManualOrderFromFile(String filePath) {
        List<String> manualOrder = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("<app>")) {
                        String packageName = line.trim().substring(5, line.trim().length() - 6);
                        manualOrder.add(packageName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "手动排序列表更新失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return manualOrder;
    }

    private void refreshAppList() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        String currentPackageName = getPackageName(); // 获取当前应用的包名

        // 从文件中加载不显示的应用包名
        hiddenAppList = loadHiddenAppListFromFile(packFilePath);

        // 从文件中加载手动排序的应用包名
        manualOrder = loadManualOrderFromFile(manualOrderFilePath);

        List<ApplicationInfo> orderedApps = new ArrayList<>();
        List<ApplicationInfo> remainingApps = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            // 排除当前应用
            if (!app.packageName.equals(currentPackageName)) {
                // 排除不显示的应用
                if (hiddenAppList != null && !hiddenAppList.contains(app.packageName)) {
                    if (manualOrder.contains(app.packageName)) {
                        // 如果应用在手动排列列表中，添加到 orderedApps
                        orderedApps.add(app);
                    } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        // 如果应用不是系统应用，添加到 remainingApps
                        remainingApps.add(app);
                    }
                }
            }
        }

        // 按手动排序列表的顺序排序
        Collections.sort(orderedApps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                return manualOrder.indexOf(o1.packageName) - manualOrder.indexOf(o2.packageName);
            }
        });

        // 按应用名称排序剩余的应用
        Collections.sort(remainingApps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                return o1.loadLabel(packageManager).toString().compareTo(o2.loadLabel(packageManager).toString());
            }
        });

        // 合并手动排序的应用和其他应用
        appList = new ArrayList<>();
        appList.addAll(orderedApps);
        appList.addAll(remainingApps);
    }

    @Override
    public void onBackPressed() {
        // 不执行任何操作，屏蔽返回键
    }

    private void loadFileContent() {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[fis.available()];
                fis.read(data);
                String content = new String(data, StandardCharsets.UTF_8);
                fileContentTextView.setText(content);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "User", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fileContentTextView.setText("User");
                }
            });
        }
    }

    private void showUserDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_user, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final EditText editText = dialogView.findViewById(R.id.editText);
        final Button saveButton = dialogView.findViewById(R.id.saveButton);
        final Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        final String filePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/userinf/user.xml";

        loadFileContentForDialog(editText);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (saveFileContentForDialog(editText, filePath)) {
                    dialog.dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog = builder.create(); // 将 dialog 赋值给成员变量
        dialog.show();
    }

    private void loadFileContentForDialog(final EditText editText) {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[fis.available()];
                fis.read(data);
                String content = new String(data, StandardCharsets.UTF_8);
                editText.setText(content);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error loading file", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private boolean saveFileContentForDialog(EditText editText, String filePath) {
        String content = editText.getText().toString();
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // 确保父目录存在

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
            loadFileContent(); // 每次回到 MainActivity 时重新加载文件内容
            refreshAppList();  // 每次回到 MainActivity 时刷新应用列表
            return true; // 保存成功
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "修改失败，请稍后重试", Toast.LENGTH_SHORT).show();
            loadFileContent(); // 每次回到 MainActivity 时重新加载文件内容
            refreshAppList();  // 每次回到 MainActivity 时刷新应用列表
            return false; // 保存失败
        }
    }
}