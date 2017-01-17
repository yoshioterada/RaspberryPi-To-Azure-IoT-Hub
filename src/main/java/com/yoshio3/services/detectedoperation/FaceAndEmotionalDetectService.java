/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.yoshio3.services.detectedoperation;

import com.yoshio3.services.PropertyReaderService;
import com.yoshio3.services.entities.EmotionRequestJSONBody;
import com.yoshio3.services.entities.EmotionResponseJSONBody;
import com.yoshio3.services.entities.FaceDetectRequestJSONBody;
import com.yoshio3.services.entities.FaceDetectResponseJSONBody;
import com.yoshio3.services.entities.MyObjectMapperProvider;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 *
 * @author Yoshio Terada
 */
public class FaceAndEmotionalDetectService {

    private static final Logger LOGGER = Logger.getLogger(FaceAndEmotionalDetectService.class.getName());
    private static final String FACE_DETECT_BASE_URI
            = "https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true"
            + "&returnFaceLandmarks=false&returnFaceAttributes=age,gender";
    private static final String EMOTIONAL_BASE_URI = "https://api.projectoxford.ai/emotion/v1.0/recognize";

    private final static String EMOTIONAL_API_SUBSCRIPTION_ID ; 
    private final static String FACE_API_SUBSCRIPTION_ID ;

    static{
        EMOTIONAL_API_SUBSCRIPTION_ID = PropertyReaderService.getPropertyValue("EMOTIONAL_API_SUBSCRIPTION_ID");
        FACE_API_SUBSCRIPTION_ID = PropertyReaderService.getPropertyValue("FACE_API_SUBSCRIPTION_ID");
    }
    
    public void showFaceAndEmotionInfoAsync(final String pictURL) throws InterruptedException, ExecutionException {
        CompletableFuture<String> faceRes = CompletableFuture.supplyAsync(execFaceDetectTask(pictURL));
        CompletableFuture<String> emoRes = CompletableFuture.supplyAsync(execEmotionDetectTask(pictURL));

        faceRes.thenAcceptBoth(emoRes, (String face, String emo) -> {
            System.out.println("*************************************************");
            System.out.println("Photo URL : " + pictURL);
            System.out.println("FACE RESPONSE :  " + face);
            System.out.println("EMOTIONAL RESPONSE :  " + emo);
            System.out.println("*************************************************");
        });
    }

    private Supplier<String> execFaceDetectTask(String pictURL) {
        Supplier<String> func = () -> {
            try {
                Future<Response> faceInfo = getFaceInfo(pictURL);
                Response faceRes = faceInfo.get();
                JsonObject jobForFace = jobForFace(faceRes);
                return jobForFace.toString();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(CameraOperation.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            }
        };
        return func;
    }

    private Supplier<String> execEmotionDetectTask(String pictURL) {
        Supplier<String> func = () -> {
            try {
                Future<Response> emotionalInfo = getEmotionalInfo(pictURL);
                Response emotionRes = emotionalInfo.get();
                JsonObject jobForEmotion = jobForEmotion(emotionRes);
                return jobForEmotion.toString();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(CameraOperation.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            }
        };
        return func;
    }

    /*
        対応ォーマット： JPEG, PNG, GIF(最初のフレーム), BMP
        画像サイズ： 4MB 以下
        検知可能な顔のサイズ：36x36 〜 4096x4096
        一画像辺り検知可能な人数：64 名
        指定可能な属性オプション(まだ実験的不正確)：
            age, gender, headPose, smile and facialHair, and glasses
            HeadPose の pitch 値は 0 としてリザーブ
     */
    private Future<Response> getFaceInfo(String pictURI)
            throws InterruptedException, ExecutionException {
        Client client = ClientBuilder.newBuilder()
                .register(MyObjectMapperProvider.class)
                .register(JacksonFeature.class)
                .build();

        WebTarget target = client.target(FACE_DETECT_BASE_URI);
        FaceDetectRequestJSONBody entity = new FaceDetectRequestJSONBody();
        entity.setUrl(pictURI);

        Future<Response> response = target
                .request(MediaType.APPLICATION_JSON)
                .header("Ocp-Apim-Subscription-Key", FACE_API_SUBSCRIPTION_ID)
                .async()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        return response;
    }

    private Future<Response> getEmotionalInfo(String pictURI) throws InterruptedException, ExecutionException {
        Client client = ClientBuilder.newBuilder()
                .register(MyObjectMapperProvider.class)
                .register(JacksonFeature.class)
                .build();
        WebTarget target = client.target(EMOTIONAL_BASE_URI);

        EmotionRequestJSONBody entity = new EmotionRequestJSONBody();
        entity.setUrl(pictURI);

        Future<Response> response = target
                .request(MediaType.APPLICATION_JSON)
                .header("Ocp-Apim-Subscription-Key", EMOTIONAL_API_SUBSCRIPTION_ID)
                .async()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        return response;
    }

    private JsonObject jobForEmotion(Response emotionRes) {
        EmotionResponseJSONBody[] persons = null;
        if (checkRequestSuccess(emotionRes)) {
            persons = emotionRes.readEntity(EmotionResponseJSONBody[].class);
        } else {
            handleIllegalState(emotionRes);
        }
        if (persons == null || persons.length < 1) {
            return null;
        }
        //現在は一人のみ解析処理
        EmotionResponseJSONBody emotionalPerson = persons[0];
        Map<String, Object> scores = emotionalPerson.getScores();

        //感情の情報を取得
        Double anger = convert((Double) scores.get("anger"));
        Double contempt = convert((Double) scores.get("contempt"));
        Double disgust = convert((Double) scores.get("disgust"));
        Double fear = convert((Double) scores.get("fear"));
        Double happiness = convert((Double) scores.get("happiness"));
        Double neutral = convert((Double) scores.get("neutral"));
        Double sadness = convert((Double) scores.get("sadness"));
        Double surprise = convert((Double) scores.get("surprise"));

        JsonObject jsonValue = Json.createObjectBuilder()
                .add("anger", anger)
                .add("contempt", contempt)
                .add("disgust", disgust)
                .add("fear", fear)
                .add("happiness", happiness)
                .add("neutral", neutral)
                .add("sadness", sadness)
                .add("surprise", surprise)
                .build();
        return jsonValue;
    }

    private JsonObject jobForFace(Response faceRes) {
        FaceDetectResponseJSONBody[] persons = null;
        if (checkRequestSuccess(faceRes)) {
            persons = faceRes.readEntity(FaceDetectResponseJSONBody[].class);
        } else {
            handleIllegalState(faceRes);
        }
        if (persons == null || persons.length < 1) {
            return null;
        }
        //現在は一人のみ解析処理
        FaceDetectResponseJSONBody faceDetectData = persons[0];

        //年齢、性別を取得
        Map<String, Object> faceAttributes = faceDetectData.getFaceAttributes();
        Double age = (Double) faceAttributes.get("age");
        String gender = (String) faceAttributes.get("gender");

        JsonObject jsonValue = Json.createObjectBuilder()
                .add("age", age)
                .add("gender", gender)
                .build();
        return jsonValue;

    }

    /* パーセント表示のためにデータをコンバート */
    private Double convert(Double before) {
        if (before == null) {
            return before;
        }
        String after = String.format("%.2f", before);
        return Double.valueOf(after) * 100;
    }

    /*
     REST 呼び出し成功か否かの判定
     */
    protected boolean checkRequestSuccess(Response response) {
        Response.StatusType statusInfo = response.getStatusInfo();
        Response.Status.Family family = statusInfo.getFamily();
        return family != null && family == Response.Status.Family.SUCCESSFUL;
    }

    /*
     REST 呼び出しエラー時の処理
     */
    private void handleIllegalState(Response response) throws IllegalStateException {
        String error = response.readEntity(String.class
        );
        LOGGER.log(Level.SEVERE, "{0}", error);
        throw new IllegalStateException(error);
    }

}
