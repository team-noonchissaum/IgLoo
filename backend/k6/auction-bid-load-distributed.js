import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Rate, Trend } from "k6/metrics";

const users = JSON.parse(open("./users.json"));

const bidCreatedRate = new Rate("bid_created_rate");
const bidBusinessRejectRate = new Rate("bid_business_reject_rate");
const bidSystemErrorRate = new Rate("bid_system_error_rate");
const bidUnexpectedResponseRate = new Rate("bid_unexpected_response_rate");
const bidLatency = new Trend("bid_latency", true);

function normalizeBaseUrl(raw) {
  if (!raw) {
    return "https://igloo-auction.duckdns.org";
  }
  if (raw.startsWith("http://") || raw.startsWith("https://")) {
    return raw;
  }
  return `https://${raw}`;
}

function parseAuctionIds(raw) {
  if (!raw) {
    return [30];
  }
  return raw
    .split(",")
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isFinite(v) && v > 0);
}

const BASE_URL = normalizeBaseUrl(__ENV.BASE_URL);
const AUCTION_IDS = parseAuctionIds(__ENV.AUCTION_IDS);
const BID_MULTIPLIER = Number(__ENV.BID_MULTIPLIER || 1.1);
const BID_STEP = Number(__ENV.BID_STEP || 10);
const EXTRA_STEPS_MAX = Number(__ENV.EXTRA_STEPS_MAX || 0);

function intEnv(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined) {
    return fallback;
  }
  const v = parseInt(raw, 10);
  return Number.isNaN(v) ? fallback : v;
}

export const options = {
  scenarios: {
    auction_bidding_distributed: {
      executor: "ramping-arrival-rate",
      startRate: intEnv("START_RATE", 20),
      timeUnit: "1s",
      preAllocatedVUs: intEnv("PRE_ALLOCATED_VUS", 200),
      maxVUs: intEnv("MAX_VUS", 2000),
      stages: [
        {
          target: intEnv("STAGE1_TARGET", 80),
          duration: __ENV.STAGE1_DURATION || "2m",
        },
        {
          target: intEnv("STAGE2_TARGET", 200),
          duration: __ENV.STAGE2_DURATION || "5m",
        },
        {
          target: intEnv("STAGE3_TARGET", 200),
          duration: __ENV.STAGE3_DURATION || "8m",
        },
        { target: 0, duration: __ENV.STAGE4_DURATION || "2m" },
      ],
    },
  },
  thresholds: {
    "http_req_duration{name:bid_place}": ["p(95)<800"],
    bid_system_error_rate: ["rate<0.01"],
    bid_unexpected_response_rate: ["rate<0.01"],
  },
};

http.setResponseCallback(http.expectedStatuses(200, 201, 400, 404, 409, 429));

function roundUpToStep(value, step) {
  return Math.ceil(value / step) * step;
}

function pickToken(setupData) {
  const idx = exec.scenario.iterationInTest % setupData.tokens.length;
  return setupData.tokens[idx];
}

function pickAuctionId() {
  const idx = exec.scenario.iterationInTest % AUCTION_IDS.length;
  return AUCTION_IDS[idx];
}

function getBidInfo(token, auctionId) {
  const res = http.get(`${BASE_URL}/api/auctions/${auctionId}`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "auction_detail" },
  });

  if (res.status !== 200) {
    return { ok: false, res };
  }

  const body = res.json();
  const data = body && body.data ? body.data : {};
  const currentPrice = Number(data.currentPrice || data.startPrice || 0);
  const bidCount = Number(data.bidCount || 0);
  const status = String(data.status || "");

  return {
    ok: true,
    currentPrice,
    bidCount,
    status,
  };
}

function isBusinessReject(status, code) {
  return (
    status === 400 ||
    status === 404 ||
    status === 409 ||
    status === 429 ||
    code === "B001" ||
    code === "B002" ||
    code === "B004" ||
    code === "B005" ||
    code === "A001" ||
    code === "A005"
  );
}

function newRequestId() {
  return `${__VU}-${__ITER}-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

function nextBidAmount(currentPrice, bidCount) {
  const minBid =
    bidCount === 0
      ? currentPrice
      : roundUpToStep(currentPrice * BID_MULTIPLIER, BID_STEP);
  const extraSteps = Math.floor(Math.random() * (EXTRA_STEPS_MAX + 1));
  return minBid + extraSteps * BID_STEP;
}

export function setup() {
  if (!Array.isArray(users) || users.length === 0) {
    throw new Error("users.json is empty. Add test users first.");
  }
  if (!Array.isArray(AUCTION_IDS) || AUCTION_IDS.length === 0) {
    throw new Error("Set AUCTION_IDS. Example: -e AUCTION_IDS=30,31,32");
  }

  const tokens = [];
  for (const user of users) {
    const loginRes = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({
        email: user.email,
        password: user.password,
        authType: "LOCAL",
      }),
      {
        headers: { "Content-Type": "application/json" },
        tags: { name: "auth_login" },
      },
    );

    if (loginRes.status !== 200) {
      const bodyPreview = String(loginRes.body || "").slice(0, 300);
      throw new Error(
        `Login failed for ${user.email}. status=${loginRes.status}, error=${loginRes.error || "none"}, error_code=${loginRes.error_code || "none"}, body=${bodyPreview}`,
      );
    }

    const accessToken = loginRes.json("accessToken");
    if (!accessToken) {
      throw new Error(`No accessToken in login response for ${user.email}`);
    }
    tokens.push(accessToken);
  }

  return { tokens };
}

export default function (setupData) {
  const token = pickToken(setupData);
  const auctionId = pickAuctionId();
  const info = getBidInfo(token, auctionId);

  if (!info.ok) {
    bidSystemErrorRate.add(info.res.status >= 500);
    bidUnexpectedResponseRate.add(info.res.status < 500);
    return;
  }

  if (info.status !== "RUNNING" && info.status !== "DEADLINE") {
    bidBusinessRejectRate.add(true);
    return;
  }

  const bidAmount = nextBidAmount(info.currentPrice, info.bidCount);
  const payload = JSON.stringify({
    auctionId,
    bidAmount,
    requestId: newRequestId(),
  });

  const res = http.post(`${BASE_URL}/api/bid`, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    tags: { name: "bid_place" },
  });

  bidLatency.add(res.timings.duration);

  const status = res.status;
  const code = res.json("code");
  const created = status === 201;
  const businessReject = isBusinessReject(status, code);
  const systemError = status >= 500;
  const unexpected = !created && !businessReject && !systemError;

  bidCreatedRate.add(created);
  bidBusinessRejectRate.add(businessReject);
  bidSystemErrorRate.add(systemError);
  bidUnexpectedResponseRate.add(unexpected);

  check(res, {
    "bid created or controlled reject": () => created || businessReject,
  });
}
