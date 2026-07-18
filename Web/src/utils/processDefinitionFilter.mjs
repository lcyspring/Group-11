/**
 * @template {{ category?: string, name?: string }} T
 * @param {T[]} definitions
 * @param {string} searchName
 * @param {string | undefined} categoryCode
 * @returns {T[]}
 */
export const filterProcessDefinitions = (definitions, searchName, categoryCode) => {
  const keyword = searchName.trim().toLocaleLowerCase()
  return definitions.filter(
    (definition) =>
      (!categoryCode || definition.category === categoryCode) &&
      (!keyword || (definition.name ?? '').toLocaleLowerCase().includes(keyword))
  )
}
