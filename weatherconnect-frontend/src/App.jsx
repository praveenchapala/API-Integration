
import { useState } from "react";
import "./App.css";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const weatherConditions = {
  0: { description: "Clear sky", icon: "☀️", theme: "sunny" },
  1: { description: "Mainly clear", icon: "🌤️", theme: "sunny" },
  2: { description: "Partly cloudy", icon: "⛅", theme: "cloudy" },
  3: { description: "Overcast", icon: "☁️", theme: "cloudy" },
  45: { description: "Fog", icon: "🌫️", theme: "cloudy" },
  48: { description: "Rime fog", icon: "🌫️", theme: "cloudy" },
  51: { description: "Light drizzle", icon: "🌦️", theme: "rainy" },
  53: { description: "Moderate drizzle", icon: "🌦️", theme: "rainy" },
  55: { description: "Dense drizzle", icon: "🌧️", theme: "rainy" },
  56: { description: "Freezing drizzle", icon: "🌧️", theme: "rainy" },
  57: { description: "Dense freezing drizzle", icon: "🌧️", theme: "rainy" },
  61: { description: "Slight rain", icon: "🌦️", theme: "rainy" },
  63: { description: "Moderate rain", icon: "🌧️", theme: "rainy" },
  65: { description: "Heavy rain", icon: "🌧️", theme: "rainy" },
  66: { description: "Freezing rain", icon: "🌧️", theme: "rainy" },
  67: { description: "Heavy freezing rain", icon: "🌧️", theme: "rainy" },
  71: { description: "Slight snowfall", icon: "🌨️", theme: "snowy" },
  73: { description: "Moderate snowfall", icon: "🌨️", theme: "snowy" },
  75: { description: "Heavy snowfall", icon: "❄️", theme: "snowy" },
  77: { description: "Snow grains", icon: "❄️", theme: "snowy" },
  80: { description: "Rain showers", icon: "🌦️", theme: "rainy" },
  81: { description: "Moderate rain showers", icon: "🌧️", theme: "rainy" },
  82: { description: "Heavy rain showers", icon: "🌧️", theme: "rainy" },
  85: { description: "Snow showers", icon: "🌨️", theme: "snowy" },
  86: { description: "Heavy snow showers", icon: "❄️", theme: "snowy" },
  95: { description: "Thunderstorm", icon: "⛈️", theme: "stormy" },
  96: { description: "Thunderstorm with hail", icon: "⛈️", theme: "stormy" },
  99: { description: "Thunderstorm with heavy hail", icon: "⛈️", theme: "stormy" },
};

function getWeatherInfo(code) {
  return (
    weatherConditions[Number(code)] || {
      description: "Unknown conditions",
      icon: "🌡️",
      theme: "cloudy",
    }
  );
}

function formatWeatherTime(value) {
  if (!value) return "Time unavailable";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function SearchIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="19"
      height="19"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      aria-hidden="true"
    >
      <circle cx="10.8" cy="10.8" r="6.8" />
      <path d="m16 16 4.2 4.2" />
    </svg>
  );
}

function LocationIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="17"
      height="17"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

function WeatherMetric({ icon, label, value, description }) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span className="metric-icon" aria-hidden="true">
          {icon}
        </span>
        <span className="metric-label">{label}</span>
      </div>
      <strong className="metric-value">{value}</strong>
      <span className="metric-description">{description}</span>
    </article>
  );
}

function App() {
  const [city, setCity] = useState("");
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function searchCity(cityName) {
    const searchedCity = cityName.trim();

    if (!searchedCity) {
      setError("Enter a city name to explore its weather.");
      setWeather(null);
      return;
    }

    setCity(searchedCity);
    setLoading(true);
    setError("");
    setWeather(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/weather?city=${encodeURIComponent(searchedCity)}`
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message ||
            data.error ||
            "Unable to retrieve weather right now."
        );
      }

      if (!data.current) {
        throw new Error("The weather service returned incomplete data.");
      }

      setWeather(data);
    } catch (err) {
      if (err instanceof TypeError) {
        setError(
          "Unable to connect to WeatherConnect. Check the backend connection and try again."
        );
      } else {
        setError(err.message || "Something went wrong. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(event) {
    event.preventDefault();
    searchCity(city);
  }

  const weatherInfo = weather
    ? getWeatherInfo(weather.current.weather_code)
    : null;

  return (
    <main className="app-shell">
      <div className="ambient ambient-one" aria-hidden="true" />
      <div className="ambient ambient-two" aria-hidden="true" />

      <header className="topbar">
        <a className="brand" href="#home" aria-label="WeatherConnect home">
          <span className="brand-mark" aria-hidden="true">
            <span>W</span>
          </span>
          <span className="brand-name">
            Weather<span>Connect</span>
          </span>
        </a>

        <div className="topbar-right">
          <span className="live-indicator" aria-hidden="true" />
          <span className="topbar-label">Your personal weather space</span>
        </div>
      </header>

      <section className="intro" id="home">
        <div className="eyebrow">
          <span className="eyebrow-line" />
          WEATHER, WITH CLARITY
        </div>

        <h1>
          A little closer
          <br />
          to <span>the sky.</span>
        </h1>

        <p className="intro-copy">
          Discover the conditions around the world, one city at a time.
          Simple insights to help you plan your day.
        </p>

        <form className="search-form" onSubmit={handleSearch}>
          <span className="search-icon">
            <SearchIcon />
          </span>

          <input
            type="text"
            value={city}
            onChange={(event) => setCity(event.target.value)}
            placeholder="Search for a city..."
            aria-label="Search for a city"
            autoComplete="off"
            maxLength={100}
          />

          <button type="submit" disabled={loading}>
            {loading ? (
              <>
                <span className="button-spinner" />
                Searching
              </>
            ) : (
              <>
                Explore <span aria-hidden="true">↗</span>
              </>
            )}
          </button>
        </form>

        <div className="quick-search">
          <span>Try a city</span>
          {["Bengaluru", "London", "Tokyo", "New York"].map((name) => (
            <button
              type="button"
              key={name}
              className="city-chip"
              disabled={loading}
              onClick={() => searchCity(name)}
            >
              {name}
            </button>
          ))}
        </div>

        {error && (
          <div className="error-banner" role="alert">
            <span className="error-symbol" aria-hidden="true">
              !
            </span>
            <span>{error}</span>
            <button
              type="button"
              className="error-dismiss"
              onClick={() => setError("")}
              aria-label="Dismiss error"
            >
              ×
            </button>
          </div>
        )}
      </section>

      {loading && (
        <section className="loading-card" aria-live="polite">
          <div className="loading-orb">☁</div>
          <div>
            <strong>Reading the sky...</strong>
            <p>Finding the latest available conditions.</p>
          </div>
          <span className="loading-dots" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
        </section>
      )}

      {weather && weatherInfo && (
        <section className="results-section" aria-live="polite">
          <div className="section-heading">
            <div>
              <span className="section-kicker">YOUR WEATHER REPORT</span>
              <h2>The sky right now</h2>
            </div>
            <span className="report-badge">
              <span className="live-indicator" />
              Current conditions
            </span>
          </div>

          <article className={`weather-hero ${weatherInfo.theme}`}>
            <div className="weather-hero-glow" aria-hidden="true" />
            <div className="weather-hero-content">
              <div className="location-line">
                <LocationIcon />
                <span>
                  {weather.city}
                  {weather.country ? `, ${weather.country}` : ""}
                </span>
              </div>

              <div className="temperature-row">
                <span className="hero-temperature">
                  {Math.round(weather.current.temperature_2m)}°
                </span>
                <div className="temperature-unit">C</div>
              </div>

              <p className="hero-condition">{weatherInfo.description}</p>
              <p className="hero-updated">
                Report time · {formatWeatherTime(weather.current.time)}
              </p>
            </div>

            <div className="hero-weather-art" aria-hidden="true">
              <span className="hero-weather-icon">{weatherInfo.icon}</span>
              <span className="orbit orbit-one" />
              <span className="orbit orbit-two" />
            </div>

            <div className="hero-coordinate">
              <span>LAT</span>
              {Number(weather.latitude).toFixed(3)}°
              <span className="coordinate-divider" />
              <span>LON</span>
              {Number(weather.longitude).toFixed(3)}°
            </div>
          </article>

          <div className="metrics-heading">
            <div>
              <span className="section-kicker">THE DETAILS</span>
              <h2>Weather at a glance</h2>
            </div>
            <p>Small details, better plans.</p>
          </div>

          <div className="metrics-grid">
            <WeatherMetric
              icon="🌡️"
              label="Temperature"
              value={`${weather.current.temperature_2m}°C`}
              description="Current air temperature"
            />
            <WeatherMetric
              icon="💧"
              label="Humidity"
              value={`${weather.current.relative_humidity_2m}%`}
              description="Moisture in the air"
            />
            <WeatherMetric
              icon="〰️"
              label="Wind speed"
              value={`${weather.current.wind_speed_10m} km/h`}
              description="Current wind movement"
            />
          </div>

          <div className="location-note">
            <span className="location-note-icon">
              <LocationIcon />
            </span>
            <div>
              <strong>Location details</strong>
              <p>
                Coordinates are provided by the weather service for{" "}
                {weather.city}.
              </p>
            </div>
            <span className="location-note-coordinates">
              {Number(weather.latitude).toFixed(2)},{" "}
              {Number(weather.longitude).toFixed(2)}
            </span>
          </div>
        </section>
      )}

      {!weather && !loading && !error && (
        <section className="welcome-panel">
          <div className="welcome-art" aria-hidden="true">
            <span className="welcome-sun" />
            <span className="welcome-cloud welcome-cloud-back" />
            <span className="welcome-cloud welcome-cloud-front" />
            <span className="welcome-star star-one">✦</span>
            <span className="welcome-star star-two">✧</span>
          </div>
          <div className="welcome-text">
            <span className="section-kicker">A WORLD OF WEATHER</span>
            <h2>Every city has its own sky.</h2>
            <p>
              Start with a city above. Get a clear picture of its temperature,
              humidity and wind in one calm, simple view.
            </p>
          </div>
          <div className="welcome-side-note">
            <span>01</span>
            <span className="welcome-side-line" />
            <span>EXPLORE</span>
          </div>
        </section>
      )}

      <footer className="footer">
        <a className="footer-brand" href="#home">
          <span className="footer-brand-mark">W</span>
          WeatherConnect
        </a>
        <span>Thoughtfully designed for curious minds.</span>
        <span className="footer-tech">React · Spring Boot</span>
      </footer>
    </main>
  );
}

export default App;
