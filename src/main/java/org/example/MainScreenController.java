package org.example;

import com.google.gson.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.currentWeather.Weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

public class MainScreenController {

    @FXML
    private Label countryLabel;

    @FXML
    private Label stateLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private Label currentTemperatureLabel;

    @FXML
    private Label windSpeedLabel;

    @FXML
    private Label preciptationLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label humidityLabel;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button searchButton;

    private JsonObject local;

    private JsonObject weather;

    private Weather nowWeather = new Weather();

    private ArrayList<String> hourlyTime = new ArrayList<String>();

    private ArrayList<Double> hourlyTemperature = new ArrayList<Double>();

    private ArrayList<Integer> hourlyHumidity = new ArrayList<Integer>();

    private ArrayList<Double> hourlyApparentTemperature = new ArrayList<Double>();

    private ArrayList<Integer> hourlyPrecipitation = new ArrayList<Integer>();

    public void searchBar() {
        String cityName = searchTextField.getText();
        if (cityName != null) {
            getLocalization(prepareCityName(cityName));
        }
        searchTextField.setText("");
    }
    public void getLocalization(String cityName) {
        try {
            URL url = new URL("https://geocoding-api.open-meteo.com/v1/search?name=" + cityName + "&count=1&language=en&format=json");
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
                local = parser.parse(jsonResponse).getAsJsonObject();

                getCords();

            } else {
                System.out.println("HTTP request failed with response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getCords() {
        if (local.has("results")) {
            JsonObject result = local.getAsJsonArray("results").get(0).getAsJsonObject();

            nowWeather.setLatitude(result.get("latitude").getAsDouble());
            nowWeather.setLongitude(result.get("longitude").getAsDouble());

            getWeather(nowWeather.getLatitude(), nowWeather.getLongitude());
        }
    }

    public void getWeather(double latitude, double longitude) {
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude="+ latitude +"&longitude="+ longitude +"&hourly=temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability&daily=weathercode,temperature_2m_max,temperature_2m_min&current_weather=true&timezone=auto");
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
                weather = parser.parse(jsonResponse).getAsJsonObject();

                extractData();
            } else {
                System.out.println("HTTP request failed with response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void extractData() {
        // Extraindo informações relacionadas a localização do alvo.
        if (local.has("results")) {
            JsonObject result = local.getAsJsonArray("results").get(0).getAsJsonObject();

            nowWeather.setCity(result.get("name").getAsString());
            nowWeather.setCountry(result.get("country").getAsString());
            nowWeather.setState(result.get("admin1").getAsString());

        }

        // Extraindo informações relacionadas ao clima do alvo.
        if (weather.has("current_weather")) {
            JsonObject currentWeatherObj = weather.get("current_weather").getAsJsonObject();

            nowWeather.setTemperature(currentWeatherObj.get("temperature").getAsDouble());
            nowWeather.setWindSpeed(currentWeatherObj.get("windspeed").getAsDouble());
            nowWeather.setIs_day(currentWeatherObj.get("is_day").getAsInt());
            nowWeather.setWeatherCode(currentWeatherObj.get("weathercode").getAsInt());
            nowWeather.setCompleteTime(currentWeatherObj.get("time").getAsString());

            extractTime(currentWeatherObj.get("time").getAsString());
            dayOfTheWeek(currentWeatherObj.get("time").getAsString());

            hourlyExtract();
            updateLabels();
        }
    }

    public void updateLabels() {
        // Essa parte configura algumas labels para UTF-8, para evitar problemas no uso de acentos.
        try {
            cityLabel.textProperty().set(new String(nowWeather.getCity().getBytes(), "UTF-8"));
            stateLabel.textProperty().set(new String(nowWeather.getState().getBytes(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        currentTemperatureLabel.setText(String.valueOf((int) Math.round(nowWeather.getTemperature())) + "°");
        preciptationLabel.setText(String.valueOf(hourlyPrecipitation.get(hourlyTime.indexOf(nowWeather.getCompleteTime()))) + "%");
        humidityLabel.setText(String.valueOf(hourlyHumidity.get(hourlyTime.indexOf(nowWeather.getCompleteTime()))) + "%");
        windSpeedLabel.setText(String.valueOf((int) Math.round(nowWeather.getWindSpeed())) + " km/h");

        countryLabel.setText(nowWeather.getCountry());

        timeLabel.setText(nowWeather.getWeekDay() + ", " + nowWeather.getDay()
                + " " + monthByNumber(nowWeather.getMonth()) + " " + nowWeather.getTime());
    }

    public void extractTime(String time) {

        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);


        nowWeather.setDay(String.valueOf(dateTime.getDayOfMonth()));
        nowWeather.setMonth(String.valueOf(dateTime.getMonthValue()));
        nowWeather.setYear(String.valueOf(dateTime.getYear()));

        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        if (hour < 10) {
            nowWeather.setTime("0"+hour + ":" + minute);
        }else {
            nowWeather.setTime(hour + ":" + minute);
        }
        if (minute < 10) {
            nowWeather.setTime(hour + ":0" + minute);
        }else {
            nowWeather.setTime(hour + ":" + minute);
        }
    }

    public void dayOfTheWeek(String time) {
        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Obter o dia da semana no estilo completo (por exemplo, "sábado")
        String diaDaSemanaCompleto = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);

        // Obter o dia da semana no estilo abreviado (por exemplo, "sáb")
        String diaDaSemanaAbreviado = dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);

        nowWeather.setWeekDay(diaDaSemanaCompleto);
    }

    public String monthByNumber(String month) {
        switch (month) {
            case "1":
                return "January";
            case "2":
                return "February";
            case "3":
                return "March";
            case "4":
                return "April";
            case "5":
                return "May";
            case "6":
                return "June";
            case "7":
                return "July";
            case "8":
                return "Augusty";
            case "9":
                return "September";
            case "10":
                return "October";
            case "11":
                return "November";
            case "12":
                return "December";
            default:
                return "";
        }
    }

    public String prepareCityName(String cityName) {
        cityName = cityName.replace(" ", "+");
        cityName = cityName.replace("ã", "a");
        cityName = cityName.replace("õ", "o");
        cityName = cityName.replace("â", "a");
        cityName = cityName.replace("ô", "o");
        cityName = cityName.replace("í", "i");
        cityName = cityName.replace("ó", "o");
        cityName = cityName.replace("é", "e");
        cityName = cityName.replace("ç", "c");

        return cityName;
    }

    public void hourlyExtract() {
        if (weather.has("hourly")) {
            JsonObject currentWeatherObj = weather.get("hourly").getAsJsonObject();

            JsonArray time = currentWeatherObj.getAsJsonArray("time");
            JsonArray temperature = currentWeatherObj.getAsJsonArray("temperature_2m");
            JsonArray humidity = currentWeatherObj.getAsJsonArray("relativehumidity_2m");
            JsonArray ApparentTemperature = currentWeatherObj.getAsJsonArray("apparent_temperature");
            JsonArray precipitation = currentWeatherObj.getAsJsonArray("precipitation_probability");

            for (int i = 0; i < time.size(); i++) {
                String time_ = time.get(i).getAsString();
                hourlyTime.add(time_);

                double temperature_ = temperature.get(i).getAsDouble();
                hourlyTemperature.add(temperature_);

                int humidity_ = humidity.get(i).getAsInt();
                hourlyHumidity.add(humidity_);

                double ApparentTemperature_ = ApparentTemperature.get(i).getAsDouble();
                hourlyApparentTemperature.add(ApparentTemperature_);

                int precipitation_ = precipitation.get(i).getAsInt();
                hourlyPrecipitation.add(precipitation_);
            }
        }
    }

}
