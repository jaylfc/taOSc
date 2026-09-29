### Fixed
- Prevent app crash when sharing while offline by catching network `IOException` in share sends and surfacing `Unreachable` instead. Also guard against revoked file access and missing agent chat channel IDs.
