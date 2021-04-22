package com.ptvinh.binance.features.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "binance-domain-client", url = "https://api.binance.com")
public interface BinanceClient {

  @GetMapping(value = "/api/v3/klines?interval=1m&limit=500", params = {"symbol"})
  List<JsonNode> getCandlesickData(@RequestParam("symbol") String symbol);
}

// api/v3/klines?interval=1m&limit=500&symbol=DOGEUSDT
//binance-api-key: lRiDTKCXSuoH3M137hiO0AC6imWfN7eZ6zG9dYCrUSgZ9MkcvuCvPEAdCxjFV0Ur
//binance-api-secret: ecyQBSNphTML6q280nms0RxPxNJFcYG8XlMMZ8QAI7ZGFtouAEvBJX2XzlL3qh3d
// signature: 327b4d31ee26f69c84284fd4f978613543de069ffd67d2ebc26219f654bb002d