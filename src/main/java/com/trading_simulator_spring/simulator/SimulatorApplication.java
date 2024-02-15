package com.trading_simulator_spring.simulator;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SimulatorApplication {

	public static void main(String[] args) {
		try (Scanner scanner = new Scanner(System.in)) {

			while (true) {
				System.out.print("=============================================================\n");
				System.out.print("Enter a coin symbol : ");
				String coinSymbol = scanner.nextLine();

				// Fetch real-time price for coinSymbol using Binance API
				double currentPrice = fetchCurrentPrice(coinSymbol);

				// If there was an error fetching the price, continue to the next iteration
				if (currentPrice == 0.0) {
					continue;
				}

				System.out.println("Current price of " + coinSymbol + ": $" + currentPrice);

				while (true) {
					System.out.print("Enter the number of days to simulate: ");
					try {
						int days = scanner.nextInt();
						scanner.nextLine(); // consume the newline

						System.out.print("Do you want the indicators data? (yes/no): ");
						String indicatorsData = scanner.nextLine().toLowerCase();

						if (indicatorsData.equals("yes") || indicatorsData.equals("y")) {
							for (int i = 1; i <= days; i++) {
								// Simulate the price with indicators for each day
								simulatePriceWithIndicators(coinSymbol, i);
							}
						} else {
							for (int i = 1; i <= days; i++) {
								// Simulate the price for each day
								System.out.print("-------------------------------------------------------------\n");
								System.out.println("Day " + i + ": ");
								simulatePrice(coinSymbol, i);
							}
						}
						break; // exit the inner loop if the number of days was entered correctly
					} catch (InputMismatchException e) {
						System.out.println("Invalid input. Please enter a number.");
						scanner.nextLine(); // consume the invalid input
					}
				}
			}
		}
	}

	private static void simulatePriceWithIndicators(String coinSymbol, int daysAgo) {
		HttpClient client = HttpClient.newHttpClient();
		long endTime = Instant.now().minus(Duration.ofDays(daysAgo)).getEpochSecond() * 1000;
		long startTime = endTime - Duration.ofDays(1).toMillis();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.binance.com/api/v3/klines?symbol=" + coinSymbol.toUpperCase()
						+ "USDT&interval=1h&startTime=" + startTime + "&endTime=" + endTime))
				.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONArray jsonArray = new JSONArray(response.body());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray candlestick = jsonArray.getJSONArray(i);
				ZonedDateTime date = Instant.ofEpochMilli(candlestick.getLong(0)).atZone(ZoneId.systemDefault());
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
				System.out.println("-------------------------------------------------------------");
				System.out.println("Date: " + date.format(formatter));
				System.out.println("Open time: " + candlestick.getLong(0));
				System.out.println("Open price: " + candlestick.getDouble(1));
				System.out.println("High price: " + candlestick.getDouble(2));
				System.out.println("Low price: " + candlestick.getDouble(3));
				System.out.println("Close price: " + candlestick.getDouble(4));
				System.out.println("Volume: " + candlestick.getDouble(5));
				System.out.println("Close time: " + candlestick.getLong(6));
				System.out.println("Quote asset volume: " + candlestick.getDouble(7));
				System.out.println("Number of trades: " + candlestick.getInt(8));
				System.out.println("Taker buy base asset volume: " + candlestick.getDouble(9));
				System.out.println("Taker buy quote asset volume: " + candlestick.getDouble(10));
				System.out.println("Ignore: " + candlestick.getString(11));
			}
		} catch (Exception e) {
			System.out.println(
					"Error fetching historical data for " + coinSymbol + ". Please ensure the coin symbol is correct.");
		}
	}

	private static void simulatePrice(String coinSymbol, int daysAgo) {
		HttpClient client = HttpClient.newHttpClient();
		long endTime = Instant.now().minus(Duration.ofDays(daysAgo)).getEpochSecond() * 1000;
		long startTime = endTime - Duration.ofDays(1).toMillis();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.binance.com/api/v3/klines?symbol=" + coinSymbol.toUpperCase()
						+ "USDT&interval=1d&startTime=" + startTime + "&endTime=" + endTime))
				.build();

		List<Map<String, String>> data = new ArrayList<>();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONArray jsonArray = new JSONArray(response.body());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray candlestick = jsonArray.getJSONArray(i);
				ZonedDateTime date = Instant.ofEpochMilli(candlestick.getLong(0)).atZone(ZoneId.systemDefault());
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

				Map<String, String> dayData = new LinkedHashMap<>();
				dayData.put("Date", date.format(formatter));
				dayData.put("Open time", String.valueOf(candlestick.getLong(0)));
				dayData.put("Open price", String.valueOf(candlestick.getDouble(1)));
				dayData.put("High price", String.valueOf(candlestick.getDouble(2)));
				dayData.put("Low price", String.valueOf(candlestick.getDouble(3)));
				dayData.put("Close price", String.valueOf(candlestick.getDouble(4)));
				dayData.put("Volume", String.valueOf(candlestick.getDouble(5)));
				dayData.put("Close time", String.valueOf(candlestick.getLong(6)));
				dayData.put("Quote asset volume", String.valueOf(candlestick.getDouble(7)));
				dayData.put("Number of trades", String.valueOf(candlestick.getInt(8)));
				dayData.put("Taker buy base asset volume", String.valueOf(candlestick.getDouble(9)));
				dayData.put("Taker buy quote asset volume", String.valueOf(candlestick.getDouble(10)));
				dayData.put("Ignore", candlestick.getString(11));
				data.add(dayData);
			}

			for (int i = 0; i < data.size(); i++) {
				for (Map.Entry<String, String> dayData : data.get(i).entrySet()) {
					System.out.print(dayData.getKey() + ": " + dayData.getValue() + "\t");
				}
				System.out.println();
			}

		} catch (Exception e) {
			System.out.println(
					"Error fetching historical data for " + coinSymbol + ". Please ensure the coin symbol is correct.");
		}
	}

	private static double fetchCurrentPrice(String coinSymbol) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(
						"https://api.binance.com/api/v3/ticker/price?symbol=" + coinSymbol.toUpperCase() + "USDT"))
				.build();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONObject json = new JSONObject(response.body());
			return json.getDouble("price");
		} catch (Exception e) {
			System.out
					.println("Error fetching price for " + coinSymbol);
			return 0.0;
		}
	}
}