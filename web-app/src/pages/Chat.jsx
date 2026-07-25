import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Box,
  Card,
  TextField,
  Typography,
  Paper,
  IconButton,
  Avatar,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
  Badge,
  CircularProgress,
  Alert,
  Stack,
} from "@mui/material";
import SendIcon from "@mui/icons-material/Send";
import AddIcon from "@mui/icons-material/Add";
import RefreshIcon from "@mui/icons-material/Refresh";
import Scene from "./Scene";
import NewChatPopover from "../components/NewChatPopover";
import {
  getMyConversations,
  createConversation,
  getMessages,
  createMessage,
} from "../services/chatService";
import { io } from "socket.io-client";
import { getToken } from "../services/localStorageService";

export default function Chat() {
  const [message, setMessage] = useState("");
  const [newChatAnchorEl, setNewChatAnchorEl] = useState(null);
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [messagesMap, setMessagesMap] = useState({});

  const messageContainerRef = useRef(null);
  const socketRef = useRef(null);

  // Ref lưu giữ selectedConversation mới nhất để tránh lỗi Stale Closure trong Socket
  const selectedConversationRef = useRef(selectedConversation);
  useEffect(() => {
    selectedConversationRef.current = selectedConversation;
  }, [selectedConversation]);

  // Hàm tự động cuộn xuống tin nhắn mới nhất
  const scrollToBottom = useCallback(() => {
    if (messageContainerRef.current) {
      messageContainerRef.current.scrollTop =
        messageContainerRef.current.scrollHeight;

      setTimeout(() => {
        if (messageContainerRef.current) {
          messageContainerRef.current.scrollTop =
            messageContainerRef.current.scrollHeight;
        }
      }, 100);
    }
  }, []);

  // Handlers mở / đóng popover tạo chat mới
  const handleNewChatClick = (event) => {
    setNewChatAnchorEl(event.currentTarget);
  };

  const handleCloseNewChat = () => {
    setNewChatAnchorEl(null);
  };

  const handleSelectNewChatUser = async (user) => {
    try {
      const response = await createConversation({
        type: "DIRECT",
        participantIds: [user.userId],
      });

      const newConversation = response?.data?.result;
      if (!newConversation) return;

      setConversations((prevConversations) => {
        const existingConversation = prevConversations.find(
          (conv) => conv.id === newConversation.id
        );
        if (existingConversation) {
          setSelectedConversation(existingConversation);
          return prevConversations;
        }
        setSelectedConversation(newConversation);
        return [newConversation, ...prevConversations];
      });
    } catch (err) {
      console.error("Error creating conversation:", err);
    } finally {
      handleCloseNewChat();
    }
  };

  // Lấy danh sách cuộc trò chuyện từ Server
  const fetchConversations = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getMyConversations();
      setConversations(response?.data?.result || []);
    } catch (err) {
      console.error("Error fetching conversations:", err);
      setError("Failed to load conversations. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConversations();
  }, []);

  // Tự động chọn cuộc trò chuyện đầu tiên nếu chưa chọn
  useEffect(() => {
    if (conversations.length > 0 && !selectedConversation) {
      setSelectedConversation(conversations[0]);
    }
  }, [conversations, selectedConversation]);

  // Tải tin nhắn của cuộc trò chuyện hiện tại
  useEffect(() => {
    if (!selectedConversation?.id) return;
    const conversationId = selectedConversation.id;

    // Đánh dấu cuộc trò chuyện đang chọn là đã đọc
    setConversations((prevConversations) =>
      prevConversations.map((conv) =>
        conv.id === conversationId ? { ...conv, unread: 0 } : conv
      )
    );

    // Chỉ fetch tin nhắn từ API nếu chưa lưu trong messagesMap
    if (!messagesMap[conversationId]) {
      getMessages(conversationId)
        .then((response) => {
          if (response?.data?.result) {
            const sortedMessages = [...response.data.result].sort(
              (a, b) => new Date(a.createdDate) - new Date(b.createdDate)
            );

            setMessagesMap((prev) => ({
              ...prev,
              [conversationId]: sortedMessages,
            }));
          }
        })
        .catch((err) => {
          console.error(
            `Error fetching messages for conversation ${conversationId}:`,
            err
          );
        });
    }
  }, [selectedConversation?.id]); // Chỉ phụ thuộc vào ID hội thoại được chọn

  const currentMessages = selectedConversation
    ? messagesMap[selectedConversation.id] || []
    : [];

  useEffect(() => {
    scrollToBottom();
  }, [currentMessages, selectedConversation, scrollToBottom]);

  // Xử lý khi nhận tin nhắn từ Socket
  const handleIncomingMessage = useCallback((incomingMsg) => {
    const activeConv = selectedConversationRef.current;

    setMessagesMap((prev) => {
      const existingMessages = prev[incomingMsg.conversationId] || [];

      // Kiểm tra nếu tin nhắn trùng lặp hoặc thay thế tin nhắn pending
      const msgIndex = existingMessages.findIndex(
        (msg) =>
          msg.id === incomingMsg.id ||
          (msg.pending && msg.message === incomingMsg.message)
      );

      let updatedMessages;
      if (msgIndex !== -1) {
        updatedMessages = [...existingMessages];
        updatedMessages[msgIndex] = incomingMsg;
      } else {
        updatedMessages = [...existingMessages, incomingMsg];
      }

      updatedMessages.sort(
        (a, b) => new Date(a.createdDate) - new Date(b.createdDate)
      );

      return {
        ...prev,
        [incomingMsg.conversationId]: updatedMessages,
      };
    });

    // Cập nhật thông tin tin nhắn cuối cùng & tin nhắn chưa đọc
    setConversations((prevConversations) =>
      prevConversations.map((conv) =>
        conv.id === incomingMsg.conversationId
          ? {
              ...conv,
              lastMessage: incomingMsg.message,
              unread:
                activeConv?.id === incomingMsg.conversationId
                  ? 0
                  : (conv.unread || 0) + 1,
              modifiedDate: incomingMsg.createdDate,
            }
          : conv
      )
    );
  }, []);

  // Khởi tạo và lắng nghe kết nối Socket.IO
  useEffect(() => {
    if (!socketRef.current) {
      const token = getToken();
      socketRef.current = io("http://localhost:8099", {
        query: { token },
      });

      socketRef.current.on("connect", () => {
        console.log("Socket connected");
      });

      socketRef.current.on("disconnect", () => {
        console.log("Socket disconnected");
      });

      socketRef.current.on("message", (rawMessage) => {
        try {
          const messageObject =
            typeof rawMessage === "string"
              ? JSON.parse(rawMessage)
              : rawMessage;
          console.log(" Received socket message:", messageObject);

          if (messageObject?.conversationId) {
            handleIncomingMessage(messageObject);
          }
        } catch (err) {
          console.error("Error parsing socket message:", err);
        }
      });
    }

    return () => {
      if (socketRef.current) {
        socketRef.current.disconnect();
        socketRef.current = null;
      }
    };
  }, [handleIncomingMessage]);

  // Gửi tin nhắn mới (Optimistic Update)
  const handleSendMessage = async () => {
    if (!message.trim() || !selectedConversation) return;

    const textToSend = message;
    const convId = selectedConversation.id;
    const tempId = `temp-${Date.now()}`;

    setMessage("");

    // Hiển thị ngay tin nhắn tạm thời lên UI (Pending)
    const tempMessage = {
      id: tempId,
      conversationId: convId,
      message: textToSend,
      me: true,
      pending: true,
      createdDate: new Date().toISOString(),
    };

    setMessagesMap((prev) => ({
      ...prev,
      [convId]: [...(prev[convId] || []), tempMessage],
    }));

    try {
      await createMessage({
        conversationId: convId,
        message: textToSend,
      });
    } catch (error) {
      console.error("Failed to send message:", error);
      // Đánh dấu tin nhắn thất bại nếu gửi lỗi
      setMessagesMap((prev) => ({
        ...prev,
        [convId]: (prev[convId] || []).map((m) =>
          m.id === tempId ? { ...m, pending: false, failed: true } : m
        ),
      }));
    }
  };

  return (
    <Scene>
      <Card
        sx={{
          width: "100%",
          height: "calc(100vh - 64px)",
          maxHeight: "100%",
          display: "flex",
          flexDirection: "row",
          mb: "-64px",
          overflow: "hidden",
        }}
      >
        {/* Conversations List */}
        <Box
          sx={{
            width: 300,
            borderRight: 1,
            borderColor: "divider",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <Box
            sx={{
              p: 2,
              borderBottom: 1,
              borderColor: "divider",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <Typography variant="h6">Chats</Typography>
            <IconButton
              color="primary"
              size="small"
              onClick={handleNewChatClick}
              sx={{
                bgcolor: "primary.light",
                color: "white",
                "&:hover": {
                  bgcolor: "primary.main",
                },
              }}
            >
              <AddIcon fontSize="small" />
            </IconButton>
            <NewChatPopover
              anchorEl={newChatAnchorEl}
              open={Boolean(newChatAnchorEl)}
              onClose={handleCloseNewChat}
              onSelectUser={handleSelectNewChatUser}
            />
          </Box>
          <Box
            sx={{
              flexGrow: 1,
              overflowY: "auto",
            }}
          >
            {(() => {
              if (loading) {
                return (
                  <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
                    <CircularProgress size={28} />
                  </Box>
                );
              }
              if (error) {
                return (
                  <Box sx={{ p: 2 }}>
                    <Alert
                      severity="error"
                      sx={{ mb: 2 }}
                      action={
                        <IconButton
                          color="inherit"
                          size="small"
                          onClick={fetchConversations}
                        >
                          <RefreshIcon fontSize="small" />
                        </IconButton>
                      }
                    >
                      {error}
                    </Alert>
                  </Box>
                );
              }
              if (conversations == null || conversations.length === 0) {
                return (
                  <Box sx={{ p: 2, textAlign: "center" }}>
                    <Typography color="text.secondary">
                      No conversations yet. Start a new chat to begin.
                    </Typography>
                  </Box>
                );
              }
              return (
                <List sx={{ width: "100%" }}>
                  {conversations.map((conversation) => (
                    <React.Fragment key={conversation.id}>
                      <ListItem
                        alignItems="flex-start"
                        onClick={() => setSelectedConversation(conversation)}
                        sx={{
                          cursor: "pointer",
                          bgcolor:
                            selectedConversation?.id === conversation.id
                              ? "rgba(0, 0, 0, 0.04)"
                              : "transparent",
                          "&:hover": {
                            bgcolor: "rgba(0, 0, 0, 0.08)",
                          },
                        }}
                      >
                        <ListItemAvatar>
                          <Badge
                            color="error"
                            badgeContent={conversation.unread}
                            invisible={!conversation.unread || conversation.unread === 0}
                            overlap="circular"
                          >
                            <Avatar
                              src={conversation.conversationAvatar || ""}
                            />
                          </Badge>
                        </ListItemAvatar>
                        <ListItemText
                          primary={
                            <Stack
                              direction="row"
                              display={"flex"}
                              justifyContent="space-between"
                              alignItems="center"
                            >
                              <Typography
                                component="span"
                                variant="body2"
                                color="text.primary"
                                noWrap
                                sx={{ display: "inline" }}
                              >
                                {conversation.conversationName}
                              </Typography>
                              <Typography
                                component="span"
                                variant="body2"
                                color="text.secondary"
                                sx={{ display: "inline", fontSize: "0.7rem" }}
                              >
                                {conversation.modifiedDate
                                  ? new Date(
                                      conversation.modifiedDate
                                    ).toLocaleString("vi-VN", {
                                      year: "numeric",
                                      month: "numeric",
                                      day: "numeric",
                                    })
                                  : ""}
                              </Typography>
                            </Stack>
                          }
                          secondary={
                            <Typography
                              sx={{ display: "inline" }}
                              component="span"
                              variant="body2"
                              color="text.primary"
                              noWrap
                            >
                              {conversation.lastMessage ||
                                "Start a conversation"}
                            </Typography>
                          }
                          primaryTypographyProps={{
                            fontWeight:
                              conversation.unread > 0 ? "bold" : "normal",
                          }}
                          sx={{
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                            pr: 1,
                          }}
                        />
                      </ListItem>
                      <Divider variant="inset" component="li" />
                    </React.Fragment>
                  ))}
                </List>
              );
            })()}
          </Box>
        </Box>

        {/* Chat Area */}
        <Box
          sx={{
            flexGrow: 1,
            display: "flex",
            flexDirection: "column",
          }}
        >
          {selectedConversation ? (
            <>
              <Box
                sx={{
                  p: 2,
                  borderBottom: 1,
                  borderColor: "divider",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                <Avatar
                  src={selectedConversation.conversationAvatar}
                  sx={{ mr: 2 }}
                />
                <Typography variant="h6">
                  {selectedConversation.conversationName}
                </Typography>
              </Box>
              <Box
                id="messageContainer"
                ref={messageContainerRef}
                sx={{
                  flexGrow: 1,
                  p: 2,
                  overflowY: "auto",
                  display: "flex",
                  flexDirection: "column",
                  position: "relative",
                }}
              >
                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    width: "100%",
                    margin: "auto 0 0 0",
                  }}
                >
                  {currentMessages.map((msg) => {
                    let backgroundColor = "#f5f5f5";
                    if (msg.me) {
                      backgroundColor = msg.failed ? "#ffebee" : "#e3f2fd";
                    }

                    return (
                      <Box
                        key={msg.id || msg.createdDate}
                        sx={{
                          display: "flex",
                          justifyContent: msg.me ? "flex-end" : "flex-start",
                          mb: 2,
                        }}
                      >
                        {!msg.me && (
                          <Avatar
                            src={msg.sender?.avatar}
                            sx={{
                              mr: 1,
                              alignSelf: "flex-end",
                              width: 32,
                              height: 32,
                            }}
                          />
                        )}
                        <Paper
                          elevation={1}
                          sx={{
                            p: 2,
                            maxWidth: "70%",
                            backgroundColor,
                            borderRadius: 2,
                            opacity: msg.pending ? 0.7 : 1,
                          }}
                        >
                          <Typography variant="body1">{msg.message}</Typography>
                          <Stack
                            direction="row"
                            spacing={1}
                            alignItems="center"
                            justifyContent="flex-end"
                            sx={{ mt: 1 }}
                          >
                            {msg.failed && (
                              <Typography variant="caption" color="error">
                                Failed to send
                              </Typography>
                            )}
                            {msg.pending && (
                              <Typography
                                variant="caption"
                                color="text.secondary"
                              >
                                Sending...
                              </Typography>
                            )}
                            <Typography
                              variant="caption"
                              sx={{ display: "block", textAlign: "right" }}
                            >
                              {msg.createdDate
                                ? new Date(msg.createdDate).toLocaleString()
                                : ""}
                            </Typography>
                          </Stack>
                        </Paper>
                        {msg.me && (
                          <Avatar
                            sx={{
                              ml: 1,
                              alignSelf: "flex-end",
                              width: 32,
                              height: 32,
                              bgcolor: "#1976d2",
                            }}
                          >
                            You
                          </Avatar>
                        )}
                      </Box>
                    );
                  })}
                </Box>
              </Box>
              <Box
                component="form"
                sx={{
                  p: 2,
                  borderTop: 1,
                  borderColor: "divider",
                  display: "flex",
                }}
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
              >
                <TextField
                  fullWidth
                  placeholder="Type a message"
                  variant="outlined"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  size="small"
                />
                <IconButton
                  color="primary"
                  sx={{ ml: 1 }}
                  onClick={handleSendMessage}
                  disabled={!message.trim()}
                >
                  <SendIcon />
                </IconButton>
              </Box>
            </>
          ) : (
            <Box
              sx={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                height: "100%",
              }}
            >
              <Typography variant="h6" color="text.secondary">
                Select a conversation to start chatting
              </Typography>
            </Box>
          )}
        </Box>
      </Card>
    </Scene>
  );
}