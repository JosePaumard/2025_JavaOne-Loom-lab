package org.paumard.server.travel.model.company.exception;


import org.paumard.server.travel.model.company.Company;

public record CompanyErrorMessage(String message, Company... companies) {
}
