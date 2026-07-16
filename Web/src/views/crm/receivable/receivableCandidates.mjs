export const filterReceivableCandidates = (candidates, customerId) =>
  customerId ? candidates.filter((candidate) => candidate.customerId === customerId) : candidates
