package org.example;

import com.google.gson.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.currentWeather.Weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
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
    private Label apparentTemperatureLabel;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button searchButton;

    @FXML
    private Button dayOneButton;

    @FXML
    private Button dayTwoButton;

    @FXML
    private Button dayThreeButton;

    @FXML
    private Button dayFourButton;

    @FXML
    private Button dayFiveButton;

    @FXML
    private Button daySixButton;

    @FXML
    private Button daySevenButton;

    @FXML
    private ImageView iconImageView;

    @FXML
    private ScrollPane pane;

    private JsonObject local;

    private JsonObject weather;

    private int index = 0;

    private Weather[] forecast = {new Weather(), new Weather(), new Weather(), new Weather(), new Weather(), new Weather(), new Weather()};

    private ArrayList<String> hourlyTime = new ArrayList<String>();

    private ArrayList<Double> hourlyTemperature = new ArrayList<Double>();

    private ArrayList<Integer> hourlyHumidity = new ArrayList<Integer>();

    private ArrayList<Double> hourlyApparentTemperature = new ArrayList<Double>();

    private ArrayList<Integer> hourlyPrecipitation = new ArrayList<Integer>();

    private ArrayList<Double> hourlyWindSpeed = new ArrayList<Double>();

    public void searchBar() {
        String cityName = searchTextField.getText();
        if (cityName != null) {
            getLocalization(prepareCityName(cityName));
        }
        searchTextField.setText("");


    }

    public void start() {

        searchBar();
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

            forecast[0].setLatitude(result.get("latitude").getAsDouble());
            forecast[0].setLongitude(result.get("longitude").getAsDouble());

            getWeather(forecast[0].getLatitude(), forecast[0].getLongitude());
        }
    }

    public void getWeather(double latitude, double longitude) {
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude="+ latitude +"&longitude="+ longitude +"&hourly=temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability,windspeed_10m&daily=weathercode,temperature_2m_max,temperature_2m_min&current_weather=true&timezone=auto");
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

            forecast[0].setCity(result.get("name").getAsString());
            forecast[0].setCountry(result.get("country").getAsString());
            forecast[0].setState(result.get("admin1").getAsString());

        }

        // Extraindo informações relacionadas ao clima do alvo.
        if (weather.has("current_weather")) {
            JsonObject currentWeatherObj = weather.get("current_weather").getAsJsonObject();

            forecast[0].setTemperature(currentWeatherObj.get("temperature").getAsDouble());
            forecast[0].setWindSpeed(currentWeatherObj.get("windspeed").getAsDouble());
            forecast[0].setIs_day(currentWeatherObj.get("is_day").getAsInt());
            forecast[0].setWeatherCode(currentWeatherObj.get("weathercode").getAsInt());
            forecast[0].setCompleteTime(currentWeatherObj.get("time").getAsString());
            forecast[0].setWeekDay(dayOfTheWeek(currentWeatherObj.get("time").getAsString(), false, true));

            extractTime(currentWeatherObj.get("time").getAsString(), true, 0);


            hourlyExtract();
            updateLabels();
        }
    }

    public void updateLabels() {
        pane.setVisible(true);
        // Essa parte configura algumas labels para UTF-8, para evitar problemas no uso de acentos.
        try {
            cityLabel.textProperty().set(new String(forecast[0].getCity().getBytes(), "UTF-8"));
            stateLabel.textProperty().set(new String(forecast[0].getState().getBytes(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        currentTemperatureLabel.setText((int) Math.round(forecast[0].getTemperature()) + "°");
        preciptationLabel.setText(hourlyPrecipitation.get(hourlyTime.indexOf(forecast[0].getCompleteTime())) + "%");
        humidityLabel.setText(hourlyHumidity.get(hourlyTime.indexOf(forecast[0].getCompleteTime())) + "%");
        windSpeedLabel.setText((int) Math.round(forecast[0].getWindSpeed()) + " km/h");
        apparentTemperatureLabel.setText((int) Math.round(hourlyApparentTemperature.get(hourlyTime.indexOf(forecast[0].getCompleteTime()))) + "°");

        countryLabel.setText(forecast[0].getCountry());

        timeLabel.setText(forecast[0].getWeekDay() + ", " + forecast[0].getDay()
                + " " + monthByNumber(forecast[0].getMonth()) + " " + forecast[0].getTime());
        iconImageView.setImage(weatherCodeIcon(forecast[0].getWeatherCode(), forecast[0].getIs_day()));

        sevenDaysForecast();
    }

    public void extractTime(String time, boolean dateTimeFormat, int index) {
        if (dateTimeFormat) {
            LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);


            forecast[index].setDay(String.valueOf(dateTime.getDayOfMonth()));
            forecast[index].setMonth(String.valueOf(dateTime.getMonthValue()));
            forecast[index].setYear(String.valueOf(dateTime.getYear()));

            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            int minute = now.getMinute();

            if (hour < 10) {
                forecast[index].setTime("0" + hour + ":" + minute);
            } else {
                forecast[index].setTime(hour + ":" + minute);
            }
            if (minute < 10) {
                forecast[index].setTime(hour + ":0" + minute);
            } else {
                forecast[index].setTime(hour + ":" + minute);
            }
        } else {
            LocalDate dateTime = LocalDate.parse(time);

            forecast[index].setDay(String.valueOf(dateTime.getDayOfMonth()));
            forecast[index].setMonth(String.valueOf(dateTime.getMonthValue()));
            forecast[index].setYear(String.valueOf(dateTime.getYear()));
        }
    }

    public String dayOfTheWeek(String time, boolean abbreviated, boolean dateTimeFormat) {
        if (dateTimeFormat) {
            LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (abbreviated) {
                String diaDaSemanaAbreviado = dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);
                return diaDaSemanaAbreviado;

            } else {
                String diaDaSemanaCompleto = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
                return diaDaSemanaCompleto;
            }
        } else {
            LocalDate dateTime = LocalDate.parse(time);
            if (abbreviated) {
                String diaDaSemanaAbreviado = dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);
                return diaDaSemanaAbreviado;

            } else {
                String diaDaSemanaCompleto = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
                return diaDaSemanaCompleto;
            }
        }
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
            clearArrays();
            JsonObject currentWeatherObj = weather.get("hourly").getAsJsonObject();

            JsonArray time = currentWeatherObj.getAsJsonArray("time");
            JsonArray temperature = currentWeatherObj.getAsJsonArray("temperature_2m");
            JsonArray humidity = currentWeatherObj.getAsJsonArray("relativehumidity_2m");
            JsonArray ApparentTemperature = currentWeatherObj.getAsJsonArray("apparent_temperature");
            JsonArray precipitation = currentWeatherObj.getAsJsonArray("precipitation_probability");
            JsonArray windSpeed = currentWeatherObj.getAsJsonArray("windspeed_10m");

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

                double windSpeed_ = windSpeed.get(i).getAsDouble();
                hourlyWindSpeed.add(windSpeed_);
            }
        }
    }

    public void forecastData() {
        if (weather.has("daily")) {
            JsonObject currentWeatherObj = weather.get("daily").getAsJsonObject();

            JsonArray time = currentWeatherObj.getAsJsonArray("time");
            JsonArray weatherCode = currentWeatherObj.getAsJsonArray("weathercode");
            JsonArray maxTemperature = currentWeatherObj.getAsJsonArray("temperature_2m_max");
            JsonArray minTemperature = currentWeatherObj.getAsJsonArray("temperature_2m_min");

            for (int i = 0; i < time.size(); i++) {
                String time_ = time.get(i).getAsString();
                forecast[i].setDate(time_);
                forecast[i].setAbbreviatedWeekDay(dayOfTheWeek(forecast[i].getDate(), true, false));
                forecast[i].setWeekDay(dayOfTheWeek(forecast[i].getDate(), false, false));
                extractTime(forecast[i].getDate(), false, i);

                int weatherCode_ = weatherCode.get(i).getAsInt();
                forecast[i].setForecastWeatherCode(weatherCode_);

                double maxTemperature_ = maxTemperature.get(i).getAsDouble();
                forecast[i].setMaxTemperature(maxTemperature_);

                double minTemperature_ = minTemperature.get(i).getAsDouble();
                forecast[i].setMimTemperature(minTemperature_);
            }
        }
    }

    public void sevenDaysForecast() {
        forecastData();
        for (int i = 0; i < 7; i++) {

            VBox vBox = new VBox(5);
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().addAll(new Label(forecast[i].getAbbreviatedWeekDay()) , new ImageView(weatherCodeIcon(forecast[i].getForecastWeatherCode(), 1)),
                    new Label((int) Math.round(forecast[i].getMaxTemperature()) + "°C\n" + (int) Math.round(forecast[i].getMimTemperature()) + "°C"));
            switch (i) {
                case 0:
                    dayOneButton.setGraphic(vBox);
                    break;
                case 1:
                    dayTwoButton.setGraphic(vBox);
                    break;
                case 2:
                    dayThreeButton.setGraphic(vBox);
                    break;
                case 3:
                    dayFourButton.setGraphic(vBox);
                    break;
                case 4:
                    dayFiveButton.setGraphic(vBox);
                    break;
                case 5:
                    daySixButton.setGraphic(vBox);
                    break;
                case 6:
                    daySevenButton.setGraphic(vBox);
                    break;
            }
        }
    }

    public Image weatherCodeIcon(int weatherCode, int is_day) {
        Image icon = null;
        if (weatherCode == 0) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/ClearSky_0.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/ClearSkyNight_0.png");
            }
        } else if (weatherCode == 51 || weatherCode == 53 || weatherCode == 55) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Drizzle_51_53_55.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/DrizzleNight_51_53_55.png");
            }
        } else if (weatherCode == 45 || weatherCode == 48) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Fog_45_48.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/FogNight_45_48.png");
            }
        } else if (weatherCode == 66 || weatherCode == 67) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/FreezingRain_66_67.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/FreezingRainNight_66_67.png");
            }
        } else if (weatherCode == 3) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Overcast_3.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/OvercastNight_3.png");
            }
        } else if (weatherCode == 1 || weatherCode == 2) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/PartlyCloudy_1_2.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/PartlyCloudyNight_1_2.png");
            }
        } else if (weatherCode == 61 || weatherCode == 63 || weatherCode == 65) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Rain_61_63_65.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/RainNight_61_63_65.png");
            }
        } else if (weatherCode == 80 || weatherCode == 81 || weatherCode == 82) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/RainShowers_80_81_82.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/RainShowersNight_80_81_82.png");
            }
        } else if (weatherCode == 71 || weatherCode == 73 || weatherCode == 75) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Snow_71_73_75.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/SnowNight_71_73_75.png");
            }
        } else if (weatherCode == 77) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/SnowGrains_77.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/SnowGrainsNight_77.png");
            }
        } else if (weatherCode == 85 || weatherCode == 86) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/SnowShowers_85_86.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/SnowShowersNight_85_86.png");
            }
        } else if (weatherCode == 95) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/Thunderstorm_95.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/ThunderstormNight_95.png");
            }
        } else if (weatherCode == 96 || weatherCode == 99) {
            if (is_day == 1) {
                icon = new Image("/iconFiles/ThunderstormRain_96_99.png");
            } else if (is_day == 0) {
                icon = new Image("/iconFiles/ThunderstormRainNight_96_99.png");
            }
        }

        return icon;
    }
    public void clearArrays() {
        hourlyTime.clear();
        hourlyTemperature.clear();
        hourlyHumidity.clear();
        hourlyApparentTemperature.clear();
        hourlyPrecipitation.clear();
    }

    public void selectForecast(ActionEvent e) {
        if (e.getSource() == dayTwoButton) {
            forecastUI(1);
        } else if (e.getSource() == dayThreeButton) {
            forecastUI(2);
        } else if (e.getSource() == dayFourButton) {
            forecastUI(3);
        } else if (e.getSource() == dayFiveButton) {
            forecastUI(4);
        } else if (e.getSource() == daySixButton) {
            forecastUI(5);
        } else if (e.getSource() == daySevenButton) {
            forecastUI(6);
        }
    }

    public void forecastUI(int day) {
        if (day == 0) {
            updateLabels();
        } else {
            timeLabel.setText(forecast[day].getWeekDay() + ", " + forecast[day].getDay()
                    + " " + monthByNumber(forecast[day].getMonth()) + " " + forecast[0].getYear());
            currentTemperatureLabel.setText((int) Math.round(forecast[day].getMaxTemperature()) + "°");
            iconImageView.setImage(weatherCodeIcon(forecast[day].getForecastWeatherCode(), 1));
        }

        if (day == 1) {
            maxHourlyData(24, 48);
        } else if (day == 2) {
            maxHourlyData(48, 72);
        } else if (day == 3) {
            maxHourlyData(72, 96);
        } else if (day == 4) {
            maxHourlyData(96, 120);
        } else if (day == 5) {
            maxHourlyData(120, 144);
        } else if (day == 6) {
            maxHourlyData(144, 168);
        }
    }

    public void maxHourlyData(int start, int end) {
        int humidity = 0;
        double apparentTemperature = 0;
        int precipitation = 0;
        double windSpeed = 0;

        for (int i = start; i < end; i++) {
            if (humidity < hourlyHumidity.get(i)) {
                humidity = hourlyHumidity.get(i);
            }
            if (apparentTemperature < hourlyApparentTemperature.get(i)) {
                apparentTemperature = hourlyApparentTemperature.get(i);
            }
            if (precipitation < hourlyPrecipitation.get(i)) {
                precipitation = hourlyPrecipitation.get(i);
            }
            if (windSpeed < hourlyWindSpeed.get(i)) {
                windSpeed = hourlyWindSpeed.get(i);
            }
        }

        humidityLabel.setText(humidity + "%");
        apparentTemperatureLabel.setText(Math.round(apparentTemperature) + "°");
        preciptationLabel.setText(precipitation + "%");
        windSpeedLabel.setText(Math.round(windSpeed) + " km/h");
    }
}
