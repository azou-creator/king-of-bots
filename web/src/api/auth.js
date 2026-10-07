import request from "@/utils/http";

export function login(data) {
  return request({
    url: "auth/account/token",
    method: "post",
    data,
  });
}

export function register(data) {
  return request({
    url: "auth/account/register",
    method: "post",
    data,
  });
}

export function getInfo(id) {
  return request({
    url: "auth/account/" + id,
    method: "get",
  });
}

export function uploadAvatar(formData) {
  return request({
    url: "auth/avatar",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
  });
}
