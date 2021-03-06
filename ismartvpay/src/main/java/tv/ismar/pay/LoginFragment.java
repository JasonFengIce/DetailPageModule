package tv.ismar.pay;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import okhttp3.ResponseBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tv.ismar.account.IsmartvActivator;
import tv.ismar.app.network.SkyService;
import tv.ismar.app.network.entity.AccountsLoginEntity;
import tv.ismar.app.widget.MessagePopWindow;

/**
 * Created by huibin on 2016/9/14.
 */
public class LoginFragment extends Fragment {
    private Button identifyCodeBtn;
    private EditText edit_identifycode;
    private Button btn_submit;
    private EditText edit_mobile;
    private TextView count_tip;

    private SkyService mSkyService;

    private View contentView;

    private PaymentActivity activity;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (PaymentActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSkyService = activity.mSkyService;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_pay_login, null);
        return contentView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }


    private void initView() {
        count_tip = (TextView) contentView.findViewById(R.id.pay_count_tip);
        edit_mobile = (EditText) contentView.findViewById(R.id.pay_edit_mobile);
        edit_identifycode = (EditText) contentView.findViewById(R.id.pay_edit_identifycode);
        identifyCodeBtn = (Button) contentView.findViewById(R.id.pay_identifyCodeBtn);
        btn_submit = (Button) contentView.findViewById(R.id.pay_btn_submit);

        btn_submit.setOnHoverListener(onHoverListener);
        identifyCodeBtn.setOnHoverListener(onHoverListener);
        edit_mobile.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
                return false;
            }
        });

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });


        identifyCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchVerificationCode();
            }
        });

    }

    public static boolean isMobileNumber(String mobiles) {
        if (mobiles.length() == 11) {
            return true;
        } else {
            return false;
        }
    }


    private void setcount_tipText(String str) {
        count_tip.setText(str + "       ");
        count_tip.setTextColor(Color.RED);
        count_tip.setVisibility(View.VISIBLE);
    }

    private void bindBestTvAuth() {
//        long timestamp = System.currentTimeMillis();
//        Activator activator = Activator.getInstance(getContext());
//        String mac = DeviceUtils.getLocalMacAddress(mcontext);
//        mac = mac.replace("-", "").replace(":", "");
//        String rsaResult = activator.PayRsaEncode("sn="
//                + SimpleRestClient.sn_token + "&timestamp=" + timestamp);
//        String params = "device_token=" + SimpleRestClient.device_token
//                + "&access_token=" + SimpleRestClient.access_token
//                + "&sharp_bestv=" + mac
//                + "&timestamp=" + timestamp + "&sign=" + rsaResult;


        String sharpBestv = "";
        String timestamp = "";
        String sign = "";

        mSkyService.accountsCombine(sharpBestv, timestamp, sign)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {

                    }
                });

    }

    private void showLoginSuccessPopup() {
        String msg = activity.getString(R.string.login_success_name);
        String phoneNumber = edit_mobile.getText().toString();
        final MessagePopWindow dialog = new MessagePopWindow(activity);
        dialog.setFirstMessage(String.format(msg, phoneNumber));
        dialog.setSecondMessage(activity.getString(R.string.login_success));
        dialog.showAtLocation(contentView, Gravity.CENTER, 0, 0, new MessagePopWindow.ConfirmListener() {
                    @Override
                    public void confirmClick(View view) {
                        dialog.dismiss();
                        PaymentActivity paymentActivity = (PaymentActivity) getActivity();
                        String model = paymentActivity.getModel();
                        int pk = paymentActivity.getPk();
                        Intent intent = new Intent(getActivity(), PaymentActivity.class);
                        intent.putExtra("model", model);
                        intent.putExtra("pk", pk);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                    }
                },
                null);
    }

    private View.OnHoverListener onHoverListener = new View.OnHoverListener() {

        @Override
        public boolean onHover(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_MOVE:
                    v.setFocusable(true);
                    v.setFocusableInTouchMode(true);
                    v.requestFocus();
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    break;
            }
            return false;
        }
    };

    private void fetchVerificationCode() {
        edit_mobile.setText("15370770697");
        String username = edit_mobile.getText().toString();
        if ("".equals(edit_mobile.getText().toString())) {
            setcount_tipText("请输入手机号");
            return;
        }
        boolean ismobile = isMobileNumber(username);
        if (!ismobile) {
            setcount_tipText("不是手机号码");
            return;
        }
//        timeCount = new IsmartCountTimer(identifyCodeBtn, R.drawable.person_btn_selector, R.drawable.btn_disabled_bg);
//        timeCount.start();
        count_tip.setVisibility(View.VISIBLE);
        edit_identifycode.requestFocus();
        count_tip.setText("60秒后可再次点击获取验证码       ");
        count_tip.setTextColor(Color.WHITE);
        count_tip.setVisibility(View.VISIBLE);


        mSkyService.accountsAuth(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        identifyCodeBtn.setEnabled(true);
                        identifyCodeBtn.setBackgroundResource(R.drawable.channel_item_normal);
                        identifyCodeBtn.setText("获取验证码");
                        setcount_tipText("获取验证码失败\n");
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        count_tip.setText("获取验证码成功，请提交!       ");
                        count_tip.setTextColor(Color.WHITE);
                        count_tip.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void doLogin() {
        String username = edit_mobile.getText().toString();
        String authNumber = edit_identifycode.getText().toString();
        if ("".equals(authNumber)) {
            setcount_tipText("验证码不能为空!");
            return;
        }
        if ("".equals(username)) {
            setcount_tipText("手机号不能为空!");
            return;
        }
        boolean ismobile = isMobileNumber(username);
        if (!ismobile) {
            setcount_tipText("不是手机号码");
            return;
        }
        mSkyService.accountsLogin(username, authNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AccountsLoginEntity>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        setcount_tipText("登录失败");
                    }

                    @Override
                    public void onNext(AccountsLoginEntity entity) {
                        String username = edit_mobile.getText().toString();
                        IsmartvActivator.getInstance().saveUserInfo(username, entity.getAuth_token(), entity.getZuser_token());
                        showLoginSuccessPopup();
                    }
                });
    }

}
