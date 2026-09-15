### Fixed

- `multi_select` taps now send the tapped value as a one-element JSON array instead of every offered option or an empty array.
- `add_note` actions now include the required `value` field alongside `note`, fixing 422 responses from the server.
