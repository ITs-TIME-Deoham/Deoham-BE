package com.deoham.card.controller;

import com.deoham.card.controller.docs.CardApplyControllerDocs;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import com.deoham.global.metrics.MetricEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.deoham.global.security.AuthenticationUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardApplyController implements CardApplyControllerDocs {

	private final CardReadService cardReadService;
	private final CardWriteService cardWriteService;

	@Override
	@MetricEndpoint("card.apply.submit")
	@PostMapping("/{cardId}/applies")
	public ResponseEntity<CardApplySummaryResponse> submitApply(
			@PathVariable UUID cardId
	) {
		UUID userId = AuthenticationUtils.requiredUserId();
		return ResponseEntity.status(HttpStatus.CREATED).body(cardWriteService.submitApply(cardId, userId));
	}

	@Override
	@MetricEndpoint("card.apply.list")
	@GetMapping("/{cardId}/applies")
	public ResponseEntity<List<CardApplySummaryResponse>> getApplies(
			@PathVariable UUID cardId
	) {
		UUID userId = AuthenticationUtils.requiredUserId();
		return ResponseEntity.ok(cardReadService.getApplies(cardId, userId));
	}
}
