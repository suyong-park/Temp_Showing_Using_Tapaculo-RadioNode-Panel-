package com.example.temp_sensor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyActivity extends AppCompatActivity {

    VerifyActivity verifyActivity;
    AlertDialog.Builder builder;
    AlertDialog con;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        setTitle("사용자 인증");

        verifyActivity = VerifyActivity.this;
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        //PreferenceManager.clear(VerifyActivity.this); // 테스트 목적의 코드 라인

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        EditText api_key = (EditText) findViewById(R.id.api_key_enter);
        EditText api_secret = (EditText) findViewById(R.id.api_secret_enter);
        EditText MAC = (EditText) findViewById(R.id.MAC_enter);
        EditText admin = (EditText) findViewById(R.id.admin_enter);
        EditText refresh = (EditText) findViewById(R.id.refresh_enter);
        Button button = (Button) findViewById(R.id.start_btn);
        Button tapaculo = (Button) findViewById(R.id.tapaculo_btn);

        tapaculo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_tapaculo = new Intent(Intent.ACTION_VIEW);
                intent_tapaculo.setData(Uri.parse("https://s2.tapaculo365.com/"));
                startActivity(intent_tapaculo);
            }
        });

        View view = (View) findViewById(R.id.linear_verify);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    Request.downKeyboard(verifyActivity);
                    return true;
                }
                return false;
            }
        });

        if(PreferenceManager.getBoolean(verifyActivity, "is_first_connect")) { // 최초 접속이 아닌 경우
            api_key.setText(PreferenceManager.getString(verifyActivity, "api_key_str"));
            api_secret.setText(PreferenceManager.getString(verifyActivity, "api_secret_str"));
            MAC.setText(PreferenceManager.getString(verifyActivity, "mac_str"));
            admin.setText(String.valueOf(PreferenceManager.getInt(verifyActivity, "admin_value")));
            refresh.setText(String.valueOf(PreferenceManager.getInt(verifyActivity, "refresh_value")));
        }

        progressDialog = new ProgressDialog(verifyActivity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog.setMessage("통신 중입니다 ...");
                progressDialog.setCancelable(false);
                progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);
                progressDialog.show();

                String api_key_str = api_key.getText().toString();
                String api_secret_str = api_secret.getText().toString();
                String mac_str = MAC.getText().toString();
                String refresh_value_str = refresh.getText().toString();
                String admin_value_str = admin.getText().toString();

                builder = new MaterialAlertDialogBuilder(verifyActivity);
                con = builder.create();
                if(api_key_str.isEmpty() || api_secret_str.isEmpty() || mac_str.isEmpty() || admin_value_str.isEmpty() || refresh_value_str.isEmpty()) {
                    progressDialog.dismiss();
                    builder.setTitle("경고")
                            .setMessage("모든 정보를 입력해 주셔야 합니다.")
                            .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                            .show();
                    return;
                }
                if(admin_value_str.length() != 4) {
                    progressDialog.dismiss();
                    Snackbar.make(view, "관리자 인증번호는 4자리입니다.", Snackbar.LENGTH_LONG).show();
                    return;
                }

                Connect_Tapaculo tapaculo = Request.getRetrofit().create(Connect_Tapaculo.class);
                Call<GetInfo> call = tapaculo.getInfo(api_key_str, api_secret_str, mac_str);
                call.enqueue(new Callback<GetInfo>() {

                    @Override
                    public void onResponse(Call<GetInfo> call, Response<GetInfo> response) {

                        System.out.println("통신 중 ...");
                        GetInfo result = response.body();
                        if (result == null) {
                            System.out.println("통신 실패");
                            progressDialog.dismiss();
                            builder.setTitle("경고")
                                    .setMessage("네트워크 연결 상태를 확인하세요.")
                                    .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                                    .show();
                            return;
                        }
                        else if (result.getStatus().equals("true")) {
                            System.out.println("통신 성공");
                            int refresh_value = Integer.parseInt(refresh_value_str);
                            int admin_value = Integer.parseInt(admin_value_str);

                            PreferenceManager.setString(verifyActivity, "api_key_str", api_key_str);
                            PreferenceManager.setString(verifyActivity, "api_secret_str", api_secret_str);
                            PreferenceManager.setString(verifyActivity, "mac_str", mac_str);
                            PreferenceManager.setInt(verifyActivity, "admin_value", admin_value);
                            PreferenceManager.setInt(verifyActivity, "refresh_value", refresh_value);
                            PreferenceManager.setBoolean(verifyActivity, "is_first_connect", true);

                            progressDialog.dismiss();
                            Intent intent = new Intent(verifyActivity, MainActivity.class);
                            startActivity(intent);
                        }
                        else { // status == false
                            System.out.println("통신 실패");
                            progressDialog.dismiss();
                            builder.setTitle("경고")
                                    .setMessage("네트워크 연결 상태를 확인하세요.")
                                    .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                                    .show();
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<GetInfo> call, Throwable t) {
                        progressDialog.dismiss();
                        builder.setTitle("경고")
                                .setMessage("네트워크 연결 상태를 확인하세요.")
                                .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                                .show();
                        return;
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(con != null && con.isShowing())
            con.dismiss();
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
