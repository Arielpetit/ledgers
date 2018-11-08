/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.ledgers.middleware.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.adorsys.ledgers.middleware.exception.NotFoundRestException;
import de.adorsys.ledgers.middleware.service.MiddlewareService;
import de.adorsys.ledgers.middleware.service.domain.payment.PaymentProductTO;
import de.adorsys.ledgers.middleware.service.domain.payment.PaymentTypeTO;
import de.adorsys.ledgers.middleware.service.domain.payment.TransactionStatusTO;
import de.adorsys.ledgers.middleware.service.exception.PaymentNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.PaymentProcessingMiddlewareException;

@RestController
@RequestMapping("/payments")
public class PaymentResource {
    private static final Logger logger = LoggerFactory.getLogger(PaymentResource.class);

    private final MiddlewareService middlewareService;

    public PaymentResource(MiddlewareService middlewareService) {
        this.middlewareService = middlewareService;
    }

    @GetMapping("/{id}/status")
    public TransactionStatusTO getPaymentStatusById(@PathVariable String id) {
        try {
            return middlewareService.getPaymentStatusById(id);
        } catch (PaymentNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage());
        }
    }

    @GetMapping(value = "/{payment-type}/{payment-product}/{paymentId}"/*, produces = {"application/json", "application/xml", "multipart/form-data"}*/)
    public ResponseEntity<?> getPaymentById(@PathVariable(name = "payment-type") PaymentTypeTO paymentType,
                                            @PathVariable(name = "payment-product") PaymentProductTO paymentProduct,
                                            @PathVariable(name = "paymentId") String paymentId) {
        try {
            return ResponseEntity.ok(middlewareService.getPaymentById(paymentType, paymentProduct, paymentId));
        } catch (PaymentNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @PostMapping("/{paymentType}")
    public ResponseEntity<?> initiatePayment(@PathVariable PaymentTypeTO paymentType, @RequestBody Object payment) {
        try {
            return new ResponseEntity(middlewareService.initiatePayment(payment, paymentType), HttpStatus.CREATED);
        } catch (Exception e) { //TODO add corresponding exceptions later (initiate payment full procedure with balance checking etc.)
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage());
        }
    }

    @GetMapping("/execute-no-sca/{payment-id}/{payment-product}/{payment-type}")
    public <T> ResponseEntity<TransactionStatusTO> executePaymentNoSca(@PathVariable(name = "payment-id") String paymentId,
                                                                       @PathVariable(name = "payment-product") PaymentProductTO paymentProduct,
                                                                       @PathVariable(name = "payment-type") PaymentTypeTO paymentType) {
        try {
            TransactionStatusTO tos = middlewareService.executePayment(paymentId);
            return ResponseEntity.ok(tos);
        } catch (PaymentProcessingMiddlewareException e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().header("message", e.getMessage()).build(); //TODO Create formal rest error messaging, fix all internal service errors to comply some pattern.
        }
    }
}
