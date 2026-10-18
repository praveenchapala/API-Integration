
import { useState } from "react";
import "./App.css";

// Convert WMO weather codes into readable descriptions and icons.
function getWeatherInfo(code) {
  const weatherConditions = {
    0: { description: "Clear sky", icon: "☀️" },
    1: { description: "Mainly clear", icon: "🌤️" },
    2: { description: "Partly cloudy", icon: "⛅" },
    3: { description: "Overcast", icon: "☁️" },
    45: { description: "Fog", icon: "🌫️" },
    48: { description: "Depositing rime fog", icon: "🌫️" },
    51: { description: "Light drizzle", icon: "🌦️" },
    53: { description: "Moderate drizzle", icon: "🌦️" },
    55: { description: "Dense drizzle", icon: "🌧️" },
    56: { description: "Light freezing drizzle", icon: "🌧️" },
    57: { description: "Dense freezing drizzle", icon: "🌧️" },
    61: { description: "Slight rain", icon: "🌦️" },
    63: { description: "Moderate rain", icon: "🌧️" },
    65: { description: "Heavy rain", icon: "🌧️" },
    66: { description: "Light freezing rain", icon: "🌧️" },
    67: { description: "Heavy freezing rain", icon: "🌧️" },
    71: { description: "Slight snowfall", icon: "🌨️" },
    73: { description: "Moderate snowfall", icon: "🌨️" },
    75: { description: "Heavy snowfall", icon: "❄️" },
    77: { description: "Snow grains", icon: "❄️" },
    80: { description: "Slight rain showers", icon: "🌦️" },
    81: { description: "Moderate rain showers", icon: "🌧️" },
    82: { description: "Violent rain showers", icon: "🌧️" },
    85: { description: "Slight snow showers", icon: "🌨️" },
    86: { description: "Heavy snow showers", icon: "❄️" },
    95: { description: "Thunderstorm", icon: "⛈️" },
    96: { description: "Thunderstorm with slight hail", icon: "⛈️" },
    99: { description: "Thunderstorm with heavy hail", icon: "⛈️" },
  };

  return (
    weatherConditions[Number(code)] ?? {
      description: "Unknown conditions",
      icon: "🌡️",
    }
  );
}

function App() {
  const [city, setCity] = useState("");
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSearch(event) {
    event.preventDefault();

    const searchedCity = city.trim();

    if (!searchedCity) {
      setError("Please enter a city name.");
      setWeather(null);
      return;
    }

    setLoading(true);
    setError("");
    setWeather(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/weather?city=${encodeURIComponent(
          searchedCity
        )}`
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error || data.message || "Unable to fetch weather."
        );
      }

      if (!data.current) {
        throw new Error("The weather service returned incomplete data.");
      }

      setWeather(data);
    } catch (err) {
      setError(
        err instanceof TypeError
          ? "Cannot connect to the backend. Check that Spring Boot is running and CORS is configured."
          : err.message || "Something went wrong. Please try again."
      );
    } finally {
      setLoading(false);
    }
  }

  const weatherInfo = weather
    ? getWeatherInfo(weather.current.weather_code)
    : null;

  return (
    <main className="app">
      <header className="header">
        <div className="brand">WeatherConnect</div>
        <span className="status">Weather Dashboard</span>
      </header>

      <section className="hero">
        <p className="eyebrow">YOUR WEATHER, SIMPLIFIED</p>
        <h1>Understand your sky.</h1>
        <p className="subtitle">
          Search any city to explore its current weather conditions.
        </p>

        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="text"
            value={city}
            onChange={(event) => setCity(event.target.value)}
            placeholder="Enter a city, e.g. Bengaluru"
            aria-label="City name"
            autoComplete="off"
          />

          <button type="submit" disabled={loading}>
            {loading ? "Searching..." : "Search"}
          </button>
        </form>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
      </section>

      {weather && (
        <section
          className="weather-card"
          aria-label={`Weather for ${weather.city}`}
        >
          <h2>
            {weather.city}
            {weather.country ? `, ${weather.country}` : ""}
          </h2>

          <div className="weather-condition">
            <span className="weather-icon" aria-hidden="true">
              {weatherInfo.icon}
            </span>
            <span>{weatherInfo.description}</span>
          </div>

          <p className="coordinates">
            Latitude: {weather.latitude} · Longitude: {weather.longitude}
          </p>

          <div className="weather-details">
            <div className="weather-detail">
              <span>Temperature</span>
              <strong>
                {weather.current.temperature_2m} °C
              </strong>
            </div>

            <div className="weather-detail">
              <span>Humidity</span>
              <strong>
                {weather.current.relative_humidity_2m}%
              </strong>
            </div>

            <div className="weather-detail">
              <span>Wind speed</span>
              <strong>
                {weather.current.wind_speed_10m} km/h
              </strong>
            </div>

            <div className="weather-detail">
              <span>Weather condition</span>
              <strong>{weatherInfo.description}</strong>
            </div>
          </div>

          <p className="weather-time">
            Weather data time: {weather.current.time}
          </p>
        </section>
      )}

      <footer className="footer">
        WeatherConnect · Built with React and Spring Boot
      </footer>
    </main>
  );
}

export default App;
