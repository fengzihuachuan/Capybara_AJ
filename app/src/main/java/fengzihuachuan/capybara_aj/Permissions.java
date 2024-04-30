package fengzihuachuan.capybara_aj;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Permissions {
    static String TAG = "Permissions";

    static String[] permissions = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO };

    static String[] permissions30 = new String[] {
            //Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO };

    static final int mRequestCode = 0x1024;

    static MainActivity mainActivity;



    public static void check(MainActivity main) {
        mainActivity = main;

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                ActivityResultLauncher<Intent> intentActivityResultLauncher =
                        mainActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (Environment.isExternalStorageManager()) {
                            request(mainActivity, permissions30);
                        } else {
                            showWaringDialog();
                        }
                    }
                });
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intentActivityResultLauncher.launch(intent);
                return;
            } else {
                request(mainActivity, permissions30);
            }
        } else {
            request(mainActivity, permissions);
        }
    }

    private static void request(MainActivity main, String[] permissions) {
        List<String> p = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++){
            if (ContextCompat.checkSelfPermission(main, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                p.add(permissions[i]);
            }
        }

        if (p.size() > 0) {
            ActivityCompat.requestPermissions(main, p.toArray(new String[p.size()]), mRequestCode);
        } else {
            mainActivity.init();
        }
    }

    public static void onResult(int requestCode, @NonNull int[] grantResults) {
        boolean hasPermissionDismiss = false;
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++){
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {
            showWaringDialog();
        } else {
            mainActivity.init();
        }
    }

    private static void showWaringDialog() {
        AlertDialog dialog = new AlertDialog.Builder(mainActivity)
                .setTitle("警告")
                .setMessage("请打开相关权限，否则功能无法正常运行.")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mainActivity.finish();
                    }
                }).show();
    }
}
