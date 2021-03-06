package com.dekist.radionodepanel;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CloudVerifyActivity extends AppCompatActivity {

    CloudVerifyActivity cloudVerifyActivity;
    AlertDialog.Builder builder;
    AlertDialog con;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_verify);
        setTitle("사용자 인증");

        cloudVerifyActivity = CloudVerifyActivity.this;

        builder = new MaterialAlertDialogBuilder(cloudVerifyActivity);
        con = builder.create();

        EditText api_key = (EditText) findViewById(R.id.api_key_enter);
        EditText api_secret = (EditText) findViewById(R.id.api_secret_enter);
        EditText search = (EditText) findViewById(R.id.search_enter);
        EditText admin = (EditText) findViewById(R.id.admin_enter);
        EditText refresh = (EditText) findViewById(R.id.refresh_enter);
        Button button = (Button) findViewById(R.id.start_btn);
        Button tapaculo = (Button) findViewById(R.id.tapaculo_btn);

        refresh.setError(null);
        admin.setError(null);

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
                    Request.downKeyboard(cloudVerifyActivity);
                    return true;
                }
                return false;
            }
        });

        if(PreferenceManager.getBoolean(cloudVerifyActivity, "is_first_connect")) { // 최초 접속이 아닌 경우
            api_key.setText(PreferenceManager.getString(cloudVerifyActivity, "api_key_str"));
            api_secret.setText(PreferenceManager.getString(cloudVerifyActivity, "api_secret_str"));
            search.setText(PreferenceManager.getString(cloudVerifyActivity, "search_str"));
            admin.setText(String.valueOf(PreferenceManager.getInt(cloudVerifyActivity, "admin_value")));
            refresh.setText(String.valueOf(PreferenceManager.getInt(cloudVerifyActivity, "refresh_value")));
        }

        progressDialog = new ProgressDialog(cloudVerifyActivity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog.setMessage("통신 중입니다 ...");
                progressDialog.setCancelable(false);
                progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);
                progressDialog.show();

                String api_key_str = api_key.getText().toString().trim();
                String api_secret_str = api_secret.getText().toString().trim();
                String search_str = search.getText().toString().trim();
                String refresh_value_str = refresh.getText().toString().trim();
                String admin_value_str = admin.getText().toString().trim();

                if(api_key_str.isEmpty() || api_secret_str.isEmpty() || search_str.isEmpty() || admin_value_str.isEmpty() || refresh_value_str.isEmpty()) {
                    progressDialog.dismiss();
                    builder.setTitle("경고")
                            .setMessage("모든 정보를 입력해 주셔야 합니다.")
                            .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                            .show();
                    return;
                }

                int refresh_temp = Integer.parseInt(refresh_value_str);
                if(1 > refresh_temp || refresh_temp > 60) {
                    progressDialog.dismiss();
                    refresh.setError(getResources().getString(R.string.refresh_error));
                    return;
                }
                else
                    refresh.setError(null);

                if(admin_value_str.length() != 4) {
                    progressDialog.dismiss();
                    admin.setError(getResources().getString(R.string.admin_error));
                    return;
                }
                else
                    admin.setError(null);

                Connect_Tapaculo tapaculo = Request.getRetrofit().create(Connect_Tapaculo.class);
                Call<GetLst> call = tapaculo.getLst(api_key_str, api_secret_str, search_str);
                call.enqueue(new Callback<GetLst>() {

                    @Override
                    public void onResponse(Call<GetLst> call, Response<GetLst> response) {

                        System.out.println("인증 통신 중 ...");
                        GetLst result = response.body();
                        if (result == null) {
                            System.out.println("인증 통신 실패");
                            progressDialog.dismiss();
                            builder.setTitle("경고")
                                    .setMessage("서버 문제 혹은 계정 OpenAPI 통신 횟수 초과의 가능성이 있습니다.\n증상이 반복되면 문의 부탁드립니다.")
                                    .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                                    .show();
                            return;
                        }
                        else if (result.getStatus().equals("true")) {
                            System.out.println("인증 통신 성공");
                            int refresh_value = Integer.parseInt(refresh_value_str);
                            int admin_value = Integer.parseInt(admin_value_str);
                            String sensors = "";

                            for(int i = 0; i < result.getRows().length; i++) {
                                String device_mac = result.getRows()[i].getDevice_mac();
                                String sensor_mac = result.getRows()[i].getSensor_mac();
                                String ch_no = result.getRows()[i].getCh_no();

                                sensors += device_mac + "-" + sensor_mac + "-CH" + ch_no;
                                if(i != result.getRows().length - 1)
                                    sensors += ",";
                                PreferenceManager.setString(cloudVerifyActivity, "ch" + i + "_name", result.getRows()[i].getCh_name());
                                PreferenceManager.setString(cloudVerifyActivity, "ch" + i + "_unit", result.getRows()[i].getCh_unit());
                            }

                            PreferenceManager.setString(cloudVerifyActivity, "device_interval", result.getRows()[0].getDevice_interval());
                            PreferenceManager.setString(cloudVerifyActivity, "sensors", sensors);

                            PreferenceManager.setString(cloudVerifyActivity, "api_key_str", api_key_str);
                            PreferenceManager.setString(cloudVerifyActivity, "api_secret_str", api_secret_str);
                            PreferenceManager.setString(cloudVerifyActivity, "search_str", search_str);
                            PreferenceManager.setInt(cloudVerifyActivity, "admin_value", admin_value);
                            PreferenceManager.setInt(cloudVerifyActivity, "refresh_value", refresh_value);
                            PreferenceManager.setBoolean(cloudVerifyActivity, "is_first_connect", true);

                            progressDialog.dismiss();
                            Intent intent = new Intent(cloudVerifyActivity, CloudMainActivity.class);
                            startActivity(intent);
                        }
                        else { // status == false
                            System.out.println("인증 통신 실패");
                            progressDialog.dismiss();
                            builder.setTitle("경고")
                                    .setMessage("네트워크 연결 상태를 확인하세요.")
                                    .setPositiveButton(getResources().getString(R.string.positive_alert), null)
                                    .show();
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<GetLst> call, Throwable t) {
                        System.out.println("인증 통신 실패");
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
    public void onBackPressed() {
        builder.setTitle("확인")
                .setMessage("정말로 선택 화면으로 나가시겠습니까?")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("취소", null)
                .setCancelable(false)
                .show();
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
