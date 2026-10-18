# WeatherConnect — Weather API Integration

WeatherConnect is a full-stack web application that retrieves current weather information for a city using external REST APIs. I built this project to understand how frontend applications communicate with backend services and how third-party API integration works in practice.

## Features

- Search for a city and retrieve its current weather.
- Display temperature, humidity, wind speed, and weather conditions.
- Retrieve city coordinates using a geocoding API.
- Handle invalid input, city-not-found errors, and external API failures.
- Deploy the frontend and backend independently.

## Tech Stack

- **Frontend:** React.js, Vite, JavaScript, CSS
- **Backend:** Java 21, Spring Boot, Spring RestClient
- **External APIs:** Open-Meteo Geocoding API and Forecast API
- **Deployment:** Vercel (frontend), Render (backend)
- **Build Tool:** Maven

## How API Integration Works

The application follows a frontend → backend → external API architecture.

1. The user enters a city in the React frontend.
2. The frontend sends an HTTP GET request to the Spring Boot backend.
3. The backend validates the city input and calls the Open-Meteo Geocoding API to find the location's latitude and longitude.
4. Using those coordinates, the backend calls the Open-Meteo Forecast API to retrieve current weather data.
5. The backend maps the response into Java DTOs and returns a structured JSON response.
6. The frontend reads the JSON response and displays the weather information.

This approach keeps external API communication in the backend and separates the user interface from business logic and external service handling.

## API Endpoints

### 1. Health Check

```http
GET /api/health
```

Checks whether the backend application is running.

Example:

```bash
curl https://weatherconnect-backend.onrender.com/api/health
```

### 2. Get Current Weather

```http
GET /api/weather?city=Bengaluru
```

Retrieves current weather information for the specified city.

Example:

```bash
curl "https://weatherconnect-backend.onrender.com/api/weather?city=Bengaluru"
```

The response contains fields such as city, country, latitude, longitude, temperature, relative humidity, weather code, and wind speed.

## Error Handling

The backend handles common failure scenarios, including:

- Empty or invalid city input.
- City not found.
- External API rate limits.
- External API connection failures and timeouts.
- Unexpected server errors.

The backend returns appropriate HTTP error responses so that the frontend can communicate failures to the user.

## Deployment

- **Frontend:** Deployed on Vercel.
- **Backend:** Deployed on Render.
- **Backend base URL:** https://weatherconnect-backend.onrender.com

The frontend uses the `VITE_API_BASE_URL` environment variable to configure the backend URL. The backend also needs to allow requests from the deployed frontend origin through CORS configuration.

## Key Learnings

Through this project, I explored:

- REST APIs and HTTP methods.
- Request parameters, JSON responses, and HTTP status codes.
- Integrating third-party APIs using Spring `RestClient`.
- Mapping external JSON responses into Java records (DTOs).
- Separating frontend, controller, API client, and exception-handling responsibilities.
- Handling external service failures and timeouts.
- Configuring CORS and environment variables.
- Deploying a full-stack application using Vercel and Render.

## Future Improvements

- Add weather forecasts for multiple days.
- Add caching and retry strategies for transient API failures.
- Improve monitoring and external API error reporting.
- Add automated integration tests.

## Project Repository

GitHub: https://github.com/praveenchapala/API-Integration
