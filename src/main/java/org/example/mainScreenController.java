package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class mainScreenController {

    @FXML
    private Label countryLabel;

    @FXML
    private Label stateLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button searchButton;

    private JsonObject jsonObject;

    public void localization() {
        try {
            URL url = new URL("https://geocoding-api.open-meteo.com/v1/search?name="+ searchTextField.getText() +"&count=1&language=pt&format=json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonResponse = response.toString();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser parser = new JsonParser();
                jsonObject = parser.parse(jsonResponse).getAsJsonObject();

                setLabels();
            } else {
                System.out.println("HTTP request failed with response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLabels() {
        if (jsonObject.has("results")) {
            JsonObject result = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject();

            countryLabel.setText(result.get("country").getAsString());
            stateLabel.setText(result.get("admin1").getAsString());
            cityLabel.setText(result.get("admin2").getAsString());

            getWeather(result.get("latitude").getAsDouble(), result.get("longitude").getAsDouble());
        }
    }

    public void getWeather(double latitude, double longitude) {

    }
}
