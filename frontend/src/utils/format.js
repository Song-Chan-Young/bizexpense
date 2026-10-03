/** 153000 -> '153,000원' */
export const formatWon = (n) => `${Number(n ?? 0).toLocaleString('ko-KR')}원`
