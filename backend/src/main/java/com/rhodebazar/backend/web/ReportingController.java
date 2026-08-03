package com.rhodebazar.backend.web;

import com.rhodebazar.backend.service.AiInsightService;
import com.rhodebazar.backend.service.AnalyseLocaleService;
import com.rhodebazar.backend.service.ReportingService;
import com.rhodebazar.backend.web.dto.AnomalieDto;
import com.rhodebazar.backend.web.dto.PrevisionVentesDto;
import com.rhodebazar.backend.web.dto.ResumeIaDto;
import com.rhodebazar.backend.web.dto.SuggestionReapprovisionnementDto;
import com.rhodebazar.backend.web.dto.TableauDeBordDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reserve a l'ADMIN : le reporting croise ventes et depenses (marge, benefice). */
@RestController
@RequestMapping("/api/reporting")
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {

    private final ReportingService reportingService;
    private final AnalyseLocaleService analyseLocaleService;
    private final AiInsightService aiInsightService;

    public ReportingController(
            ReportingService reportingService,
            AnalyseLocaleService analyseLocaleService,
            AiInsightService aiInsightService) {
        this.reportingService = reportingService;
        this.analyseLocaleService = analyseLocaleService;
        this.aiInsightService = aiInsightService;
    }

    @GetMapping("/dashboard")
    public TableauDeBordDto tableauDeBord(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate finEffective = fin != null ? fin : LocalDate.now();
        LocalDate debutEffectif = debut != null ? debut : finEffective.minusDays(29);
        return reportingService.calculerTableauDeBord(debutEffectif, finEffective);
    }

    @GetMapping("/previsions")
    public PrevisionVentesDto previsions() {
        return analyseLocaleService.prevoirVentes();
    }

    @GetMapping("/reapprovisionnement")
    public List<SuggestionReapprovisionnementDto> reapprovisionnement() {
        return analyseLocaleService.suggererReapprovisionnement();
    }

    @GetMapping("/anomalies")
    public List<AnomalieDto> anomalies() {
        return analyseLocaleService.detecterAnomaliesDepenses();
    }

    @GetMapping("/resume-ia")
    public ResumeIaDto resumeIa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate finEffective = fin != null ? fin : LocalDate.now();
        LocalDate debutEffectif = debut != null ? debut : finEffective.minusDays(29);
        TableauDeBordDto tableau = reportingService.calculerTableauDeBord(debutEffectif, finEffective);
        PrevisionVentesDto prevision = analyseLocaleService.prevoirVentes();
        return aiInsightService.genererResume(tableau, prevision);
    }
}
