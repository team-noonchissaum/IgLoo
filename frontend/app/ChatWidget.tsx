'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  fetchChatScenarios,
  startChatScenario,
  chatNext,
  callChatAction,
} from '@/lib/api';
import {
  ApiResponse,
  ChatAction,
  ChatNext,
  ChatNode,
  ChatScenarioSummary,
} from '@/lib/types';

type Message = {
  id: string;
  role: 'bot' | 'user';
  text: string;
};

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [scenarios, setScenarios] = useState<ChatScenarioSummary[]>([]);
  const [currentNode, setCurrentNode] = useState<ChatNode | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const scenarioMode = useMemo(() => !currentNode, [currentNode]);

  useEffect(() => {
    if (!open) return;
    setError('');
    setLoading(true);
    setMessages([{ id: 'bot-greet', role: 'bot', text: '안녕하세요! 무엇을 도와드릴까요?' }]);
    fetchChatScenarios()
      .then((res: ApiResponse<ChatScenarioSummary[]>) => {
        setScenarios(res.data || []);
      })
      .catch((e) => {
        console.error(e);
        setError('시나리오 목록을 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, [open]);

  const startScenario = async (scenarioId: number) => {
    setError('');
    setLoading(true);
    try {
      const res: ApiResponse<ChatNode> = await startChatScenario(scenarioId);
      const node = res.data;
      setCurrentNode(node);
      setMessages([{ id: `bot-${node.nodeId}`, role: 'bot', text: node.text }]);
    } catch (e) {
      console.error(e);
      setError('시나리오 시작에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleOption = async (nodeId: number, optionId: number, label: string) => {
    setMessages((prev) => [
      ...prev,
      { id: `user-${Date.now()}`, role: 'user', text: label },
    ]);

    setLoading(true);
    setError('');
    try {
      const res: ApiResponse<ChatNext> = await chatNext(nodeId, optionId);
      const next = res.data;

      if (next.type === 'NODE' && next.node) {
        setCurrentNode(next.node);
        setMessages((prev) => [
          ...prev,
          { id: `bot-${next.node.nodeId}`, role: 'bot', text: next.node.text },
        ]);
        return;
      }

      if (next.type === 'ACTION' && next.action) {
        const actionMessage = await runAction(next.action);
        setMessages((prev) => [
          ...prev,
          { id: `bot-${Date.now()}`, role: 'bot', text: actionMessage },
        ]);
        return;
      }

      setError('다음 단계를 찾지 못했습니다.');
    } catch (e) {
      console.error(e);
      setError('처리에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const runAction = async (action: ChatAction) => {
    if (action.actionType === 'LINK') {
      if (action.actionTarget) {
        if (action.actionTarget.startsWith('http')) {
          window.open(action.actionTarget, '_blank');
          return '새 탭에서 이동했습니다.';
        }
        window.location.href = action.actionTarget;
        return '페이지로 이동했습니다.';
      }
      return '이동할 링크가 없습니다.';
    }

    if (action.actionType === 'API') {
      if (!action.actionTarget) return '호출할 API가 없습니다.';
      try {
        await callChatAction(action.actionTarget);
        return 'API 호출을 완료했습니다.';
      } catch (e) {
        console.error(e);
        return 'API 호출에 실패했습니다.';
      }
    }

    return '처리할 액션이 없습니다.';
  };

  const resetChat = () => {
    setCurrentNode(null);
    setMessages([{ id: 'bot-greet', role: 'bot', text: '안녕하세요! 무엇을 도와드릴까요?' }]);
    setError('');
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full border border-black bg-white text-black shadow-lg hover:bg-gray-100"
        aria-label="Open chat"
      >
        Chat
      </button>

      {open && (
        <div className="fixed bottom-28 right-6 z-50 w-96 rounded-xl border border-black bg-white text-black shadow-2xl">
          <div className="flex items-center justify-between border-b border-black px-4 py-3">
            <div className="font-semibold">IgLoo 챗봇</div>
            <div className="flex gap-2">
              <button
                onClick={resetChat}
                className="text-xs text-black hover:text-gray-700"
              >
                초기화
              </button>
              <button
                onClick={() => setOpen(false)}
                className="text-xs text-black hover:text-gray-700"
              >
                닫기
              </button>
            </div>
          </div>

          <div className="max-h-[520px] overflow-y-auto px-4 py-3 space-y-3">
            {error && (
              <div className="rounded border border-black bg-white px-3 py-2 text-sm text-black">
                {error}
              </div>
            )}

            {messages.map((m) => (
              <div
                key={m.id}
                className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[80%] rounded-lg border border-black px-3 py-2 text-sm ${
                    m.role === 'user'
                      ? 'bg-white text-black'
                      : 'bg-white text-black'
                  }`}
                >
                  {m.text}
                </div>
              </div>
            ))}

            {scenarioMode && (
              <div className="space-y-2">
                <div className="text-sm text-black">
                  아래에서 원하시는 도움을 선택해 주세요.
                </div>
                {loading && <div className="text-xs text-black">불러오는 중...</div>}
                {scenarios.map((s) => (
                  <button
                    key={s.scenarioId}
                    onClick={() => startScenario(s.scenarioId)}
                    className="w-full rounded-lg border border-black px-3 py-2 text-left text-sm hover:bg-gray-50"
                  >
                    <div className="font-medium">{s.title}</div>
                    {s.description && (
                      <div className="text-xs text-black">{s.description}</div>
                    )}
                  </button>
                ))}
              </div>
            )}

            {!scenarioMode && currentNode && (
              <div className="space-y-2">
                {currentNode.options.map((opt) => (
                  <button
                    key={opt.optionId}
                    onClick={() => handleOption(currentNode.nodeId, opt.optionId, opt.label)}
                    className="w-full rounded-lg border border-black px-3 py-2 text-left text-sm hover:bg-gray-50"
                    disabled={loading}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
