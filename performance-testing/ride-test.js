import http from 'k6/http';
import { check } from "k6";

let url = "http://localhost:8082/payment"

let uuid = "d5d5f708-b50f-42b9-ab4b-ec3f1a4df5d3"

export const options = {
    scenarios: {
        contacts: {
            executor: "per-vu-iterations",
            vus: 50,
            iterations: 1,
            maxDuration: "30s",
        }
    }
}


export default function () {

   let data = {
       tripId: "77040b24-5cfd-49c1-8a93-80496bd476fb",
       paymentAmount: 50
   }

   let res = http.post(url, JSON.stringify(data),
       {
          headers: {
             "Content-Type": "application/json",
             "Idempotency-Key": uuid
          }
       }
   )

    check(res, {
        "Status is 201 CREATED (WENT THROUGH)": (r) => r.status === 201,
        "Status is 409 CONFLICT (LOCKED OUT)": (r) => r.status === 409,
    })

}
