package com.trading_simulator_spring.simulator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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

	public static void main(String[] args) throws IOException, InterruptedException {
		try (Scanner scanner = new Scanner(System.in)) {
			HttpClient client = HttpClient.newHttpClient();

			while (true) {
				System.out.print("=============================================================\n");
				System.out.print("Enter a coin symbol : ");
				String input = scanner.nextLine();
				if (input.trim().isEmpty() || input.matches("\\d+")) {
					System.out.println("Input cannot be empty. Please enter a coin symbol.");
					continue;
				}
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create("https://api.binance.com/api/v3/klines?symbol=" + input.toUpperCase()
								+ "USDT&interval=" + "4h"))
						.build();

				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				String responseBody = response.body();
				if (responseBody.startsWith("{")) {
					JSONObject jsonObject = new JSONObject(responseBody);
					if (jsonObject.has("code")) {
						System.out.println(jsonObject.getString("msg"));
						continue;
					}
				}

				String coinSymbol = input.split(" ")[0];
				String interval = "4h"; // default interval

				Pattern pattern = Pattern.compile("([a-zA-Z]+)\\s*(1s|1m|3m|5m|15m|30m|1h|4h|6h|8h|12h|1d|3d|1w)?");
				Matcher matcher = pattern.matcher(input);
				if (matcher.matches()) {
					coinSymbol = matcher.group(1);
					if (matcher.group(2) != null) {
						interval = matcher.group(2);
					}
				}

				while (true) {
					System.out.print("Enter the number of days to simulate: ");
					String input2 = scanner.nextLine();
					if (input2.trim().isEmpty()) {
						System.out.println("Input cannot be empty. Please enter a number.");
						continue;
					}
					if (!input2.matches("\\d+")) {
						System.out.println("Invalid input. Please enter a number.");
						continue;
					}
					int days = Integer.parseInt(input2);

					for (int i = 1; i <= days; i++) {
						// Simulate the price for each day
						System.out.print("-------------------------------------------------------------\n");
						System.out.println("Day " + i + ": ");
						simulatePrice(coinSymbol, i, interval);
					}
					break; // exit the loop if the number of days was entered correctly
				}
			}
		}
	}

	private static void simulatePrice(String coinSymbol, int daysAgo, String interval) {
		HttpClient client = HttpClient.newHttpClient();
		long endTime = Instant.now().minus(Duration.ofDays(daysAgo)).getEpochSecond() * 1000;
		long startTime = endTime - Duration.ofDays(1).toMillis();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.binance.com/api/v3/klines?symbol=" + coinSymbol.toUpperCase()
						+ "USDT&interval=" + interval + "&startTime=" + startTime + "&endTime=" + endTime))
				.build();

		List<Map<String, String>> data = new ArrayList<>();
		List<Double> closePrices = new ArrayList<>(); // Define closePrices here

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JSONArray jsonArray = new JSONArray(response.body());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray candlestick = jsonArray.getJSONArray(i);
				ZonedDateTime date = Instant.ofEpochMilli(candlestick.getLong(0)).atZone(ZoneId.systemDefault());
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
				double closePrice = candlestick.getDouble(4);
				closePrices.add(closePrice);
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
			double rsi = calculateRSI(closePrices);
			for (Map<String, String> dayData : data) {
				dayData.put("RSI", String.valueOf(rsi));
			}

		} catch (Exception e) {
		}

	}

	private static double calculateRSI(List<Double> closePrices) {
		int period = 14;
		List<Double> gains = new ArrayList<>();
		List<Double> losses = new ArrayList<>();

		for (int i = 1; i < closePrices.size(); i++) {
			double change = closePrices.get(i) - closePrices.get(i - 1);
			gains.add(Math.max(0, change));
			losses.add(Math.max(0, -change));
		}

		double avgGain = gains.stream().limit(period).mapToDouble(n -> n).average().orElse(0);
		double avgLoss = losses.stream().limit(period).mapToDouble(n -> n).average().orElse(0);

		if (closePrices.size() <= period) {
			return 0; // Return 0 if there are not enough close prices to calculate the RSI
		}

		for (int i = period; i < closePrices.size(); i++) {
			double gain = gains.get(i);
			double loss = losses.get(i);
			avgGain = ((avgGain * (period - 1)) + gain) / period;
			avgLoss = ((avgLoss * (period - 1)) + loss) / period;
			double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
			double rsi = 100 - (100 / (1 + rs));
			return rsi;
		}

		return 0; // This line is not necessary, but it's here to satisfy the Java compiler
	}
}