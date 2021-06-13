package com.xz.tool.network;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.internal.$Gson$Types;
import com.xz.tool.network.cookie.PersistenceCookieJar;
import com.xz.tool.network.utils.DateFormat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author czr
 * @email czr2001@outlook.com
 * @date 2021/6/13
 * <p>
 * 基于Okhttp3 请求的工具类
 * 回调数据主动回到主线程
 */
public class OKHttpClickManager {
	private static final String TAG = OKHttpClickManager.class.getName();
	private static OKHttpClickManager mInstance;
	private OkHttpClient mOkHttpClient;
	private Handler mDelivery;
	private Gson mGson;

	private OKHttpClickManager() {
		OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
		okHttpBuilder.connectTimeout(15, TimeUnit.SECONDS);
		okHttpBuilder.writeTimeout(15, TimeUnit.SECONDS);
		okHttpBuilder.readTimeout(15, TimeUnit.SECONDS);
		//是否自动重连
		okHttpBuilder.retryOnConnectionFailure(false);
		//自动管理cookie ---待修复 自动管理cookie
		//okHttpBuilder.cookieJar(new CookieJar() {
		//	//这里一定一定一定是HashMap<String, List<Cookie>>,是String,不是url.
		//	private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
		//
		//	@Override
		//	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		//		cookieStore.put(url.host(), cookies);
		//	}
		//
		//	@Override
		//	public List<Cookie> loadForRequest(HttpUrl url) {
		//		List<Cookie> cookies = cookieStore.get(url.host());
		//		return cookies != null ? cookies : new ArrayList<Cookie>();
		//
		//
		//	}
		//});


		okHttpBuilder.cookieJar(new PersistenceCookieJar());

		//禁制OkHttp的重定向操作
		//okHttpBuilder.followRedirects(false);
		//okHttpBuilder.followSslRedirects(false);


		//添加https证书
		//loadCert(okHttpBuilder);
		//信任所有证书  不推荐使用
		trustAll(okHttpBuilder);

		mOkHttpClient = okHttpBuilder.build();
		mDelivery = new Handler(Looper.getMainLooper());
		mGson = new GsonBuilder()
				.setDateFormat(DateFormat.NORM_DATETIME_PATTERN)//gson解析date类型，这里的date类型对应服务器的yyyy-MM-dd HH:mm:ss
				.create();
	}

	public static OKHttpClickManager getInstance() {
		if (mInstance == null) {
			synchronized (OKHttpClickManager.class) {
				if (mInstance == null) {
					mInstance = new OKHttpClickManager();
				}
			}
		}
		return mInstance;
	}


	/**
	 * 信任所有证书
	 */
	private void trustAll(OkHttpClient.Builder okHttpBuilder) {
		//信任所有服务器地址
		okHttpBuilder.hostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String s, SSLSession sslSession) {
				//设置为true
				return true;
			}
		});
		//创建管理器
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] x509Certificates,
					String s) throws java.security.cert.CertificateException {
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] x509Certificates,
					String s) throws java.security.cert.CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[]{};
			}
		}};
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

			//为OkHttpClient设置sslSocketFactory
			okHttpBuilder.sslSocketFactory(sslContext.getSocketFactory());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	///**
	// * 添加https证书
	// *
	// * @param okHttpBuilder
	// */
	//private void loadCert(OkHttpClient.Builder okHttpBuilder) {
	//	//设置https配置
	//	X509TrustManager trustManager;
	//	SSLSocketFactory sslSocketFactory;
	//	final InputStream inputStream;
	//	//添加https证书
	//	try {
	//
	//		inputStream = BaseApplication.getInstance().getAssets().open("certs/TestCert.cer"); // 得到证书的输入流
	//		try {
	//
	//			trustManager = trustManagerForCertificates(inputStream);//以流的方式读入证书
	//			SSLContext sslContext = SSLContext.getInstance("TLS");
	//			sslContext.init(null, new TrustManager[]{trustManager}, null);
	//			sslSocketFactory = sslContext.getSocketFactory();
	//
	//		} catch (GeneralSecurityException e) {
	//			e.printStackTrace();
	//			throw new RuntimeException(e);
	//		}
	//		okHttpBuilder.sslSocketFactory(sslSocketFactory, trustManager);
	//	} catch (IOException e) {
	//		e.printStackTrace();
	//	}
	//
	//}


	private X509TrustManager trustManagerForCertificates(InputStream in)
			throws GeneralSecurityException {
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
		if (certificates.isEmpty()) {
			throw new IllegalArgumentException("expected non-empty set of trusted certificates");
		}

		// Put the certificates a key store.
		char[] password = "xzlyf297".toCharArray(); // Any password will work.
		KeyStore keyStore = newEmptyKeyStore(password);
		int index = 0;
		for (Certificate certificate : certificates) {
			String certificateAlias = Integer.toString(index++);
			keyStore.setCertificateEntry(certificateAlias, certificate);
		}

		// Use it to build an X509 trust manager.
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
				KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, password);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:"
					+ Arrays.toString(trustManagers));
		}
		return (X509TrustManager) trustManagers[0];
	}


	/**
	 * 添加password
	 *
	 * @param password
	 * @return
	 * @throws GeneralSecurityException
	 */
	private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); // 这里添加自定义的密码，默认
			InputStream in = null; // By convention, 'null' creates an empty key store.
			keyStore.load(in, password);
			return keyStore;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}


	/**
	 * 为HttpGet 的 url 方便的添加多个name value 参数。
	 *
	 * @param url
	 * @param params
	 * @param isHavFirst 是否需要问号
	 * @return
	 */
	public static String attachHttpGetParams(String url, Map<String, Object> params, boolean isHavFirst) {
		if (params == null) {
			return url;
		}
		Iterator<String> keys = params.keySet().iterator();
		Iterator<Object> values = params.values().iterator();
		StringBuffer stringBuffer = new StringBuffer();
		if (isHavFirst) {
			stringBuffer.append("?");
		} else {
			stringBuffer.append("&");
		}


		for (int i = 0; i < params.size(); i++) {
			String value = null;
			try {
				value = URLEncoder.encode(values.next().toString(), "utf-8");
			} catch (Exception e) {
				e.printStackTrace();
			}

			stringBuffer.append(keys.next() + "=" + value);
			if (i != params.size() - 1) {
				stringBuffer.append("&");
			}
		}
		//Logger.e("HttpGetParams = " + url + stringBuffer);//请求连接
		return url + stringBuffer.toString();
	}


	/**
	 * 处理请求和返回
	 *
	 * @param request
	 */
	private void deliveryRequest(Request request, final ResultCallback callback) {
		Call call = mOkHttpClient.newCall(request);
		call.enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
					//如果是主动取消的情况下
				} else {
					//                    sendFailedStringCallback(request, e, callback);
					sendFailedStringCallback(call.request(), e, callback);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try {
					final String string = response.body().string();
					if (callback.mType == String.class) {
						sendSuccessResultCallback(string, callback);
					} else {
						Object o = mGson.fromJson(string, callback.mType);
						sendSuccessResultCallback(o, callback);
					}
				} catch (IOException | JsonParseException e) {
					sendFailedStringCallback(response.request(), e, callback);
				}
			}
		});
	}


	private void sendFailedStringCallback(final Request request, final Exception e, final ResultCallback callback) {
		mDelivery.post(new Runnable() {
			@Override
			public void run() {
				if (callback != null)
					callback.onError(request, e);
			}
		});
	}

	private void sendSuccessResultCallback(final Object object, final ResultCallback callback) {
		mDelivery.post(new Runnable() {
			@Override
			public void run() {
				if (callback != null) {
					callback.onResponse(object);
				}
			}
		});
	}

	/**
	 * 结果回调接口
	 *
	 * @param <T>
	 */
	public static abstract class ResultCallback<T> {
		Type mType;

		public ResultCallback() {
			mType = getSuperclassTypeParameter(getClass());
		}

		static Type getSuperclassTypeParameter(Class<?> subclass) {
			Type superclass = subclass.getGenericSuperclass();
			if (superclass instanceof Class) {
				throw new RuntimeException("Missing type parameter.");
			}
			ParameterizedType parameterized = (ParameterizedType) superclass;
			return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
		}

		public abstract void onError(Request request, Exception e);

		public abstract void onResponse(T response);
	}

	/**
	 * 通用GET请求
	 * 可供外部调用
	 */
	private Request buildGetCommonRequest(String url,
	                                      Map<String, Object> query,
	                                      Map<String, Object> header,
	                                      Object tag) {
		Request.Builder builder = new Request.Builder();
		builder.url(attachHttpGetParams(url, query, true));
		if (header != null) {
			for (Map.Entry<String, Object> entry : header.entrySet()) {
				builder.addHeader(entry.getKey(), entry.getValue().toString());
			}
		}
		if (tag != null) {
			builder.tag(tag);
		}
		return builder.build();
	}

	/**
	 * 通用POST请求
	 * 可供外部调用
	 */
	private Request buildPostCommonRequest(String url,
	                                       Map<String, Object> body,
	                                       Map<String, Object> header,
	                                       Map<String, Object> query,
	                                       Object tag) {
		//填装body
		FormBody.Builder builder = new FormBody.Builder();
		if (body != null) {
			for (Map.Entry<String, Object> entry : body.entrySet()) {
				builder.add(entry.getKey(), entry.getValue().toString());
			}
		}
		RequestBody requestBody = builder.build();

		//填装header
		Request.Builder requestBuilder = new Request.Builder();
		if (header != null) {
			for (Map.Entry<String, Object> entry : header.entrySet()) {
				requestBuilder.addHeader(entry.getKey(), entry.getValue().toString());
			}
		}
		//填装query
		requestBuilder.url(attachHttpGetParams(url, query, false));
		requestBuilder.post(requestBody);
		//配置tag
		if (tag != null) {
			requestBuilder.tag(tag);
		}
		return requestBuilder.build();
	}


	/*
	 * ==========================公开工具方法===========================
	 */

	public OkHttpClient getOkHttpClient() {
		return mOkHttpClient;
	}

	/**
	 * 获取会话id
	 *
	 * @param headers 请求头
	 * @return session id
	 */
	public String getSessionId(Headers headers) {
		List<String> values = headers.values("Set-Cookie");
		if (values.size() == 0) {
			return "";
		}
		String session = values.get(0);
		return session.substring(0, session.indexOf(";"));
	}

	/**
	 * 取消所有请求
	 */
	public void cancelAll() {
		if (mOkHttpClient == null) {
			return;
		}
		mOkHttpClient.dispatcher().cancelAll();
	}

	/**
	 * 根据Tag取消请求
	 */
	public void cancelTag(Object tag) {
		if (mOkHttpClient == null || tag == null) return;
		for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
			if (tag.equals(call.request().tag())) {
				call.cancel();
			}
		}
		for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
			if (tag.equals(call.request().tag())) {
				call.cancel();
			}
		}
	}


	/*
	 * ==========================公开请求方法===========================
	 */

	/**
	 * 自定义request请求头进行请求
	 *
	 * @param request  自定义请求
	 * @param callback 结果回调
	 */
	public void customRequest(Request request, ResultCallback callback) {
		deliveryRequest(request, callback);
	}

	/**
	 * 通用GET请求
	 *
	 * @param baseUrl  baseUrl
	 * @param query    Query参数，无参数可以null
	 * @param callback 结果回调
	 * @param tag      Call标签
	 */
	public void get(String baseUrl,
	                Map<String, Object> header,
	                Map<String, Object> query,
	                Object tag,
	                ResultCallback callback) {
		Request request = buildGetCommonRequest(baseUrl, query, header, null);
		deliveryRequest(request, callback);
	}


	/**
	 * 通用POST请求
	 *
	 * @param baseUrl  baseUrl
	 * @param header   Header参数，无参数可以null
	 * @param query    Query参数，无参数可以null
	 * @param body     Body参数，无参数可以null
	 * @param tag      Call标签
	 * @param callback 结果回调
	 */
	public void post(String baseUrl,
	                 Map<String, Object> header,
	                 Map<String, Object> query,
	                 Map<String, Object> body,
	                 Object tag,
	                 ResultCallback callback) {
		Request request = buildPostCommonRequest(baseUrl, body, header, query, tag);
		deliveryRequest(request, callback);
	}


}
