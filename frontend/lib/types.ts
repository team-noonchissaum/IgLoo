export interface ApiResponse<T> {
  message: string;
  data: T;
}

export interface Auction {
  auctionId: number;
  title: string;
  description: string;
  currentPrice: number;
  startPrice: number;
  bidCount: number;
  status: 'WAITING' | 'READY' | 'RUNNING' | 'ENDED' | 'CANCELED';
  startAt: string;
  endAt: string;
  sellerNickname: string;
  imageUrls: string[];
  categoryId: number;
  categoryName: string;
}

export interface AuctionRegisterRequest {
  title: string;
  description: string;
  startPrice: number;
  categoryId: number;
  startAt: string;
  endAt: string;
  imageUrls: string[];
}

export type ChatActionType = 'NONE' | 'LINK' | 'API';

export interface ChatScenarioSummary {
  scenarioId: number;
  title: string;
  description?: string;
}

export interface ChatOption {
  optionId: number;
  label: string;
  nextNodeId?: number | null;
  actionType: ChatActionType;
  actionTarget?: string | null;
}

export interface ChatNode {
  nodeId: number;
  scenarioId: number;
  text: string;
  terminal: boolean;
  options: ChatOption[];
}

export interface ChatAction {
  actionType: ChatActionType;
  actionTarget?: string | null;
}

export interface ChatNext {
  type: 'NODE' | 'ACTION';
  node?: ChatNode | null;
  action?: ChatAction | null;
}
