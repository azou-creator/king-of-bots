import request from "@/utils/http";
import qs from "qs";

export function getRecordList(params) {
  return request({
    url: "/record/list" + qs.stringify(params, { addQueryPrefix: true }),
    method: "get",
  });
}
