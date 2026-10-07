### Fixed

- Android share sender now treats permanent 4xx responses (except 408 and 429) as Rejected instead of Unreachable, preventing multi-hour retries for refused shares.
