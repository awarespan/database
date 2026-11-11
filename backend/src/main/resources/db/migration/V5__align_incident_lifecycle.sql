-- Align persisted incident lifecycle and deterrability values with the Android domain model.
ALTER TABLE incidents DROP CONSTRAINT incidents_state_check;
ALTER TABLE incidents
    ADD CONSTRAINT incidents_state_check CHECK (state IN (
        'DETECTED',
        'EVIDENCE_CAPTURED',
        'PENDING_VERIFICATION',
        'AUTONOMOUS_EVALUATION',
        'AUTO_HANDLED',
        'VERIFIED',
        'REJECTED',
        'DETERRENCE_ACTIVE',
        'DETERRENCE_COMPLETED',
        'ESCALATED',
        'RESOLVED'
    ));

ALTER TABLE incidents DROP CONSTRAINT incidents_threat_severity_check;
UPDATE incidents
SET threat_severity = 'DETERRABLE'
WHERE threat_severity IN ('LOW', 'MEDIUM', 'HIGH');
ALTER TABLE incidents
    ADD CONSTRAINT incidents_threat_severity_check
    CHECK (threat_severity IN ('DETERRABLE', 'NON_DETERRABLE'));
