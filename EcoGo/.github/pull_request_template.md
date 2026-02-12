# Pull Request Template

## Description
Please include a summary of the changes and related context. Include any relevant motivation and context.

Closes # (issue number)

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing Done
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Local security scans passed

## Checklist
- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have verified that new and existing unit tests pass locally
- [ ] Security scans (Checkstyle, SpotBugs, Dependency Check) pass
- [ ] No secrets or sensitive information committed

## Screenshots (if applicable)
<!-- Add screenshots here if relevant -->

## Deployment Notes
<!-- Add any special deployment instructions or considerations -->

## Security Considerations
- [ ] No hardcoded secrets
- [ ] Input validation applied
- [ ] SQL injection prevention verified
- [ ] XSS prevention verified
- [ ] No new vulnerabilities introduced

## Performance Impact
- [ ] No performance impact
- [ ] Performance improvement (describe)
- [ ] Performance regression accepted (justify)

## Breaking Changes
Describe any breaking changes here. If none, state "None."

## Additional Context
Add any other context about the PR here.

---

**Note**: This PR will trigger:
- LINT checks (Checkstyle)
- SAST scans (SpotBugs, OWASP Dependency Check)
- Code quality analysis (SonarQube)
- Unit and integration tests

Once approved and merged to `main`, it will automatically:
- Build Docker image
- Deploy to staging environment
- Run DAST security testing
- Set up monitoring with Prometheus and Grafana
