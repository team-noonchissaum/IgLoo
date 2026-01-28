const BASE_URL = 'http://localhost:8080/api';

// 임의의 유저 ID (백엔드 X-User-Id 헤더 대응)
const CURRENT_USER_ID = '1';

export async function fetchAuctions(status?: string) {
  const url = status 
    ? `${BASE_URL}/auctions?status=${status}` 
    : `${BASE_URL}/auctions`;
  
  const res = await fetch(url, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch auctions');
  return res.json();
}

export async function fetchAuctionDetail(id: string) {
  const res = await fetch(`${BASE_URL}/auctions/${id}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch auction detail');
  return res.json();
}

export async function registerAuction(data: any) {
  const res = await fetch(`${BASE_URL}/auctions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': CURRENT_USER_ID,
    },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to register auction');
  return res.json();
}

export async function cancelAuction(id: number) {
  const res = await fetch(`${BASE_URL}/auctions/${id}`, {
    method: 'DELETE',
    headers: {
      'X-User-Id': CURRENT_USER_ID,
    },
  });
  if (!res.ok) throw new Error('Failed to cancel auction');
  return res.json();
}
