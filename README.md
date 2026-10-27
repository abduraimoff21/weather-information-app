# Weather Information App

A desktop weather app written in Java. Type a city (or coordinates) and it shows the current
weather, a 5-day forecast, weather icons and a background that follows the time of day at that
place. It is built with **Java Swing** and gets its data from the free **Open-Meteo** weather
API. It needs no libraries and no API key.

## Features

| Requirement | How the app meets it |
|---|---|
| API integration | `WeatherService` calls the Open-Meteo **geocoding** API (name to coordinates) and **forecast** API (current weather and daily forecast) over HTTPS and reads the JSON replies. |
| GUI design (Swing) | One window with a search box, a **Search** button, unit switches, a message bar, the weather cards and a history list. Enter also starts a search. |
| Location input | A city name (`Paris`, `Paris, France`, `Paris, Texas`) **or** coordinates (`48.85, 2.35`). |
| Weather information | Temperature, feels-like temperature, humidity, wind speed and direction, conditions, pressure, sunrise and sunset, and the local time of the searched place. |
| Icons | Sun, moon, partly cloudy, clouds, fog, drizzle, rain, snow and thunderstorm icons, drawn with Java 2D (sun by day, moon by night). |
| Forecast | Five day cards: weekday and date, icon, high / low temperature, conditions and chance of precipitation. |
| Unit conversion | Temperature: **°C / °F**. Wind speed: **km/h, mph, m/s, knots**. Switching is instant and does not call the API again. |
| Error handling | Invalid input (empty, unusable characters, bad coordinates), unknown places, no internet connection, timeouts, and API errors each show a clear message in a red bar. The app never crashes and keeps the last result on screen. |
| History | The last 20 searches with date and time, saved to a file so they survive a restart. Click an entry to look it up again; **Clear history** empties the list. |
| Dynamic background | The sky changes between **sunrise**, **day**, **sunset** and **night**, based on the sunrise and sunset times of the searched place (sun, moon and stars are drawn). |

## Requirements

* **Java 17 or newer** (developed and tested with OpenJDK 21). Swing is part of the JDK.
* An internet connection (the weather data comes from the web).
* No API key, no external libraries and no build tool.

## How to run

Open a terminal in the project folder (the one that contains `src/`).

**1. Compile**

```
javac -encoding UTF-8 -d out src/weatherapp/*.java
```

**2. Start the app**

```
java -cp out weatherapp.WeatherApp
```

## How to use it

1. Type a place in the search box: a **city** such as `Tashkent`, a city with a country or
   state to be exact such as `Paris, France` or `Paris, Texas`, or **coordinates** as
   `latitude, longitude` such as `48.85, 2.35` (south and west are negative: `-33.87, 151.21`).
2. Press **Search** (or Enter). The message bar shows the progress and then the result.
3. Read the **current weather** card and the **5-day forecast**. Hover over a forecast icon for
   the conditions.
4. Use the **Temperature** and **Wind speed** switches to change units. The display updates at
   once.
5. Click an entry under **Recent searches** to see that place again. **Clear history** removes
   all entries.
6. The window background shows what time it is *at the searched place*: a glowing dawn sky, a
   blue day sky, a red and orange sunset, or a starry night with the moon.

Search history is stored in `.weather-information-app/history.tsv` in your home folder. Start
the app with `-Dweatherapp.history=C:\path\to\file.tsv` to use another file.

## The weather API and its usage guidelines

The app uses [Open-Meteo](https://open-meteo.com/):

| Purpose | Request |
|---|---|
| Place name to coordinates | `GET https://geocoding-api.open-meteo.com/v1/search?name=<city>&count=1&language=en&format=json` |
| Weather for coordinates | `GET https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&timezone=auto&forecast_days=5` |

**API key.** Open-Meteo's free API does not use keys, so there is no secret in the code or in
the repository, and the project runs as soon as it is compiled. (Services such as
OpenWeatherMap need a personal key. `WeatherService` is the only class that knows about the
web service, so another provider would only change that class.)

**Usage guidelines followed** (from open-meteo.com/en/terms and /en/licence, checked on 27 October 2026):

* *Non-commercial use only.* This is a free educational project with no ads or subscriptions.
* *Rate limits* of the free tier are 600 calls per minute, 5,000 per hour and 10,000 per day.
  The app makes one call per search for coordinates and two for a city name, never polls or
  refreshes by itself, and ignores a second search while one is still running. Changing units
  never calls the API.
* *Attribution (CC BY 4.0).* The window always shows a clickable "Weather data by
  Open-Meteo.com" link next to the data.
* Requests send an identifying `User-Agent` header and use HTTPS.

## How it works

| Class | Responsibility |
|---|---|
| `WeatherApp` | Entry point; starts the window on the Swing event thread. |
| `WeatherFrame` | The main window: controls, searching on a background thread (`SwingWorker`), showing results and errors. |
| `WeatherService` | Calls the two Open-Meteo APIs and converts the replies into Java objects; turns every failure into a `WeatherException`. |
| `Json` | A small JSON reader (the project needs no library). |
| `InputParser`, `LocationQuery` | Validate what the user typed and decide whether it is a city name or coordinates. |
| `Location`, `CurrentConditions`, `DailyForecast`, `WeatherReport` | Immutable data records. |
| `TemperatureUnit`, `WindSpeedUnit` | Unit conversion. Data is stored in °C and km/h and converted when displayed. |
| `DisplayFormat` | Turns numbers and dates into display text (English format on every computer). |
| `WeatherCode`, `IconType`, `WeatherIcon` | Translate weather codes to text and icons; draw the icons with Java 2D. |
| `TimeOfDay`, `BackgroundPanel` | Choose sunrise, day, sunset or night from the sun times, and paint the matching sky. |
| `SearchHistory` | The list of recent searches with timestamps, saved to a text file. |
| `CurrentWeatherPanel`, `ForecastPanel`, `HistoryPanel`, `StatusBar` | The cards of the window. |
| `RoundedPanel`, `HintTextField`, `AccentButton`, `SegmentedControl`, `ThinScrollBarUI`, `Ui` | Small reusable pieces of the look and feel. |

Design decisions:

* **The window never freezes.** Network calls run in a `SwingWorker`; the result is shown back on
  the event thread. The Search button is disabled while a search runs.
* **Convert at display time.** Storing metric values and converting when drawing makes unit
  switches instant and avoids repeated rounding errors.
* **One exception type for expected problems.** `WeatherException` carries a type (invalid
  input, not found, network, service) and a message written for the user. Unexpected bugs are
  logged to the console and shown as a generic message.
* **Dynamic background by the sun, not the clock.** `TimeOfDay.at(now, sunrise, sunset)` uses the
  place's own local time and its sunrise and sunset: sunrise is 45 minutes before to 60 minutes
  after sunrise, sunset is 75 minutes before to 45 minutes after sunset, night is the rest. If the
  sun times are missing, the clock is used (before the first search the computer's clock is used).

### Error messages

| Situation | Message (shortened) |
|---|---|
| Empty search | Invalid input: Please enter a city name or coordinates first. |
| Unusable characters, e.g. `!!!` | Invalid input: Use only letters, numbers, spaces and hyphens in a city name, or coordinates like 48.85, 2.35. |
| Coordinates out of range, e.g. `95, 10` | Invalid input: Latitude must be between -90 and 90 degrees. |
| Unknown place | Location not found: No place called "..." was found. Check the spelling, or add the country. |
| No internet / server unreachable | Connection problem: Could not reach the weather service. Check your internet connection. |
| Slow server | Connection problem: The weather service took too long to answer. |
| HTTP error, rate limit, bad reply | Weather service problem: ... |

## Tests

```
javac -encoding UTF-8 -d out src/weatherapp/*.java tests/weatherapp/*.java
java -cp out weatherapp.TestRunner
```

`TestRunner` needs no test library. It runs **240 checks**: unit conversion, number and date
formatting, weather codes and icons, the time-of-day rules, the JSON reader (including malformed
input), input validation, the search history file, the service against a local mock server (every
error case: HTTP 400, 429, 500, bad JSON, missing data, timeouts, refused connections, unknown
hosts), the window itself (searches, errors, unit switching, backgrounds, history) and the real
Open-Meteo API. The results are saved in `tests/output/test_results.txt`.

`java -cp out weatherapp.Screenshots docs/screenshots` starts the real window, drives it and saves
the screenshots used in the report (needs a display and an internet connection).

## Project layout

```
src/weatherapp/     the application (24 classes)
tests/weatherapp/   TestRunner (automated checks) and Screenshots (screenshot tool)
tests/output/       saved test results
docs/screenshots/   screenshots of the running application
docs/build_report.py   builds the Word/PDF report
```

## Credits

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0). Place names come from
[GeoNames](https://www.geonames.org/) through the Open-Meteo geocoding API.
