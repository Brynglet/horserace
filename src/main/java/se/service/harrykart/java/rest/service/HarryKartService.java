package se.service.harrykart.java.rest.service;

import se.service.harrykart.java.rest.pojo.HarryResponse;

public interface HarryKartService {
    HarryResponse getTopFinishers(String xmlStr);
}
