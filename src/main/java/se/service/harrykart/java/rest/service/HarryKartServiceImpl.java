package se.service.harrykart.java.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.service.harrykart.java.generated.HarryKartType;
import se.service.harrykart.java.generated.LaneType;
import se.service.harrykart.java.generated.ParticipantType;
import se.service.harrykart.java.rest.pojo.HarryResponse;
import se.service.harrykart.java.rest.pojo.HorseDTO;
import se.service.harrykart.java.rest.pojo.PositionHorse;
import se.service.harrykart.java.rest.utility.XmlConverter;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static se.service.harrykart.java.rest.utility.CommonConstants.*;

@Slf4j
@Service
public class HarryKartServiceImpl implements HarryKartService {

    @Override
    public HarryResponse getTopFinishers(String xmlStr) {

        var harryKartType = XmlConverter.transformXmlToJava(xmlStr);

        List<HorseDTO> horseDTOs = harryKartType.getStartList().getParticipant()
                .stream()
                .map(participantType -> getHorseDto(participantType, harryKartType))
                .sorted((horseDTO1, horseDTO2) -> horseDTO1.getTotalTime() > horseDTO2.getTotalTime() ? 1 : -1)
                .limit(NR_OF_MEDAL_FINISHERS)
                .collect(Collectors.toList());

        return convertToResponse(horseDTOs);
    }

    private HorseDTO getHorseDto(ParticipantType participantType, HarryKartType hkt) {

        Double totalTime = getTotalRaceTime(participantType, hkt);

        return HorseDTO.builder()
                .horseName(participantType.getName())
                .laneNr(participantType.getLane())
                .totalTime(totalTime)
                .build();
    }

    private Double getTotalRaceTime(ParticipantType participantType, HarryKartType hkt) {

        List<BigInteger> bumpUps = hkt.getPowerUps().getLoop()
                .stream()
                .sorted((loopType1, loopType2) ->
                        loopType1.getNumber().intValue() > loopType2.getNumber().intValue() ? 1 : -1)
                .flatMap(loopType -> loopType.getLane().stream())
                .filter(laneType -> participantType.getLane().intValue() == laneType.getNumber().intValue())
                .map(LaneType::getValue)
                .collect(Collectors.toList());

        double lapSpeed = getLapSpeed(participantType.getBaseSpeed().doubleValue(), DOUBLE_ZERO);
        double totalTime = LAP_LENGTH / lapSpeed;

        for (BigInteger bumpUp : bumpUps) {
            lapSpeed = getLapSpeed(lapSpeed, bumpUp.doubleValue());
            totalTime += LAP_LENGTH / lapSpeed;
        }

        return totalTime;
    }

    private Double getLapSpeed(Double currentSpeed, Double extraSpeed) {

        double lapSpeed = currentSpeed + extraSpeed;

        if (lapSpeed <= 0) {
            log.error(ZonedDateTime.now() + ". getLapSpeed exception lapSpeed <= 0 :" + lapSpeed);
            throw new RuntimeException("getLapSpeed exception lapSpeed <= 0 :" + lapSpeed);
        }

        return lapSpeed;
    }

    private HarryResponse convertToResponse(List<HorseDTO> horseDTOs) {

        AtomicInteger pos = new AtomicInteger();

        List<PositionHorse> responseInfo = horseDTOs
                .stream()
                .sorted((horseDTO1, horseDTO2) -> horseDTO1.getTotalTime() > horseDTO2.getTotalTime() ? 1 : -1)
                .map(horseDTO -> PositionHorse.builder()
                        .horse(horseDTO.getHorseName())
                        .position(pos.incrementAndGet())
                        .totalTime(horseDTO.getTotalTime())
                        .build())
                .collect(Collectors.toList());

        return HarryResponse.builder()
                .ranking(responseInfo)
                .build();
    }
}
