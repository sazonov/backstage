package com.proit.app.controller;

import com.proit.app.controller.request.CoffeeRequest;
import com.proit.app.controller.request.NumberTypeRequest;
import com.proit.app.exception.AppException;
import com.proit.app.model.api.ApiStatusCodeImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class SimpleTestController
{
	@GetMapping("/ping")
	public ResponseEntity<String> ping()
	{
		return ResponseEntity.ok("pong");
	}

	@PostMapping("/order")
	public ResponseEntity<?> order(@RequestBody CoffeeRequest request)
	{
		return ResponseEntity.ok(request);
	}

	@PostMapping("/calculate")
	public ResponseEntity<?> calculate(@RequestBody NumberTypeRequest request)
	{
		return ResponseEntity.ok(request);
	}

	@GetMapping("/exception")
	public ResponseEntity<?> exception() throws Exception
	{
		throw new Exception("Throwing test exception!", new UnsupportedOperationException("Empty method"));
	}

	@GetMapping("/appException")
	public ResponseEntity<?> appException() throws Exception
	{
		throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT);
	}
}
