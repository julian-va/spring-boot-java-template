---
name: security
description: Apply backend and frontend security practices covering OWASP Top 10, input validation, authn/authz, secrets management, secure headers, SCA/SAST/DAST scanning and a security PR checklist.
version: 0.1.0
tags: [security, backend, frontend, best-practices]
---

# SKILL: Security Best Practices (Backend & Frontend) 🔐

## Purpose

This skill collects recommended security practices for both backend and frontend development. It is intended for humans and automated agents to validate, review, and implement secure features and fixes consistently across projects.

---

## Scope

- Backend services (APIs, microservices, workers)
- Frontend applications (SPAs, server-rendered pages)
- CI/CD pipelines, containers, and deployment artifacts

---

## Core principles

- Adopt a defense-in-depth approach: multiple layered mitigations.
- Follow least privilege: grant minimal permissions by default.
- Fail securely: default-deny, avoid leaking sensitive info in errors.
- Keep dependencies and tooling updated; scan for vulnerabilities continuously.
- Use threat modeling for significant features and changes.

---

## General requirements

- Reference OWASP Top 10 and SANS/CWE as part of design and testing.
- Include security considerations in design docs and PR descriptions.
- Return the Reasoning Checklist (if required) before making code changes:
  - Threats identified & impact
  - Design choices and mitigations
  - Data classification and handling
  - Dependencies and SCA results

---

## Backend-specific practices

- Input validation & output encoding: validate all inputs on the server side and encode outputs to avoid injection attacks.
- Use parameterized queries / prepared statements for DB access; avoid string concatenation for SQL.
- Authenticate & authorize: use proven standards (OAuth2, OIDC, JWT) and enforce scope/role checks on every protected resource.
- Secrets management: do NOT store secrets in code or plaintext; use managed secret stores (Vault, AWS Secrets Manager, GitHub Secrets).
- Secure defaults: secure configs by default and require explicit opt-in for less secure features.
- Rate limiting & throttling: protect endpoints from abuse and brute force attempts.
- Secure deserialization: avoid native deserialization of untrusted data; use safe parsers and explicit schemas.
- File uploads: validate type/size, store outside webroot, scan for malware and validate file contents.
- Logging: redact sensitive fields (passwords, tokens, PII) and avoid logging raw secrets; maintain audit logs for critical actions.
- Error handling: return minimal information to clients; log full details only to secure internal logs.
- Dependency scanning: run SCA (e.g., Dependabot, Snyk) and SAST tools in CI.
- Container & runtime security: sign images, scan for vulnerabilities (Trivy), run with least privileges, and set resource limits.
- Data protection: encrypt sensitive data at rest and in transit (TLS 1.2+ with strong ciphers).
- Secrets rotation and revocation policies.

---

## Frontend-specific practices

- Content Security Policy (CSP): define and enforce a CSP to mitigate XSS and data injection. Use nonce or strict hashes for inline scripts when necessary.
- XSS mitigation: escape untrusted content, sanitize HTML on the server where possible, avoid using innerHTML with untrusted content.
- Secure storage: avoid storing tokens in localStorage; prefer secure, HttpOnly cookies for session tokens or use short-lived tokens with secure storage patterns.
- CSRF protection: use SameSite cookies or anti-CSRF tokens for state-changing requests.
- CORS: configure CORS to allow only required origins; avoid wildcard origins in production.
- SRI (Subresource Integrity) for third-party scripts when served from CDNs.
- OAuth PKCE for native and SPAs when using OAuth flows.
- Minimize exposure of sensitive data in the DOM, HTML titles, or metadata.
- Use HTTPS everywhere and ensure HSTS is set for production domains.
- Secure headers: set X-Frame-Options, X-Content-Type-Options, Referrer-Policy, and Strict-Transport-Security.

---

## CI/CD & infrastructure

- Run automated security checks in CI: linters, mypy/typing for backend, SAST, and dependency scanning.
- Run DAST in a staging environment (OWASP ZAP or similar).
- Protect pipelines: restrict who can modify CI configs and GitHub Actions secrets; require code review for workflow changes.
- Image provenance: build reproducible images, sign them, and verify signatures in deployment.
- Use infrastructure-as-code with reviewable diffs and run automated checks for insecure configurations.

---

## Testing & validation

- Add security-focused tests and regression tests for mitigations (e.g., XSS and SQL injection tests).
- Include baseline SAST and SCA results in PRs when appropriate.
- Perform regular penetration testing and bug bounty programs if applicable.

---

## Observability & incident response

- Instrument logging and monitoring for security events (failed auth, suspicious activity, privilege escalations).
- Retain audit logs as required by data policy and ensure secure access to logs.
- Define incident response playbooks and run tabletop exercises periodically.

---

## Accessibility & privacy interplay

- Ensure privacy-by-design: minimize collection of PII, anonymize/aggregate where possible, and follow regulations (GDPR, CCPA) as applicable.
- Balance security controls with accessibility: e.g., alternative flows for users with assistive tech.

---

## PR checklist (Security)

- [ ] Threat model or short rationale attached for non-trivial changes
- [ ] Input validation and encoding in place for all user-supplied data
- [ ] Authentication and authorization enforced for protected endpoints
- [ ] Secrets are not present in code/commits; secrets reviewed via SCA
- [ ] Dependency scan results checked and addressed
- [ ] CSP and secure headers configured for frontend changes
- [ ] DAST/SAST jobs run in CI for the PR
- [ ] Logging avoids PII and secrets; audit logs enabled for critical flows

---

## Tools & resources

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/
- OWASP ZAP: https://www.zaproxy.org/
- Snyk: https://snyk.io/
- Trivy: https://aquasecurity.github.io/trivy/
- Dependabot (GitHub) and GitHub Advanced Security
