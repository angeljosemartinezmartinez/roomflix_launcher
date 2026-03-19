<?php

namespace App\Controller;

use App\Entity\Device;
use App\Entity\Status;
use FOS\RestBundle\Controller\AbstractFOSRestController;
use FOS\RestBundle\Controller\Annotations as Rest;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

/**
 * API MDM: registro de dispositivos y heartbeat (RoomFlix launcher).
 * deviceId en el body JSON corresponde a la MAC del dispositivo (12 caracteres).
 *
 * @Route("/api/mdm")
 */
class MdmController extends AbstractFOSRestController
{
    /**
     * Registra un dispositivo nuevo o actualiza sus datos.
     * Body JSON: deviceId (MAC, obligatorio), opcional: model, manufacturer, androidVersion, sdkInt, appVersion, appVersionCode.
     *
     * @Rest\Post("/register", name="api_mdm_register")
     */
    public function register(Request $request): Response
    {
        $params = $this->getPostData($request);
        if (empty($params['deviceId'])) {
            return new JsonResponse(['error' => 'deviceId is required'], Response::HTTP_BAD_REQUEST);
        }

        $mac = $this->normalizeMac($params['deviceId']);
        if (strlen($mac) !== 12) {
            return new JsonResponse(['error' => 'deviceId must be a 12-character MAC'], Response::HTTP_BAD_REQUEST);
        }

        $em = $this->getDoctrine()->getManager();
        $device = $em->getRepository(Device::class)->findOneBy(['mac' => $mac]);

        if (!$device) {
            $device = new Device();
            $device->setMac($mac);
            $device->setNamePoint('mdm_' . substr($mac, -3));
            $device->setPassPoint((string) random_int(10000000, 99999999));
            $device->setAccessPoint(false);
            $device->setNetworkType($params['networkType'] ?? 0);
            $device->setIsActive(true);

            $status = new Status();
            $status->setDevice($device);
            $status->setDate(new \DateTime());
            $status->setStatus($this->buildStatusPayload($params));
            $status->setAppsUninstall([]);

            $device->setStatus($status);
            $em->persist($device);
            $em->persist($status);
        } else {
            $status = $device->getStatus();
            if (!$status) {
                $status = new Status();
                $status->setDevice($device);
                $status->setAppsUninstall([]);
                $device->setStatus($status);
                $em->persist($status);
            }
            $status->setDate(new \DateTime());
            $currentStatus = $status->getStatus() ?? [];
            $status->setStatus(array_merge($currentStatus, $this->buildStatusPayload($params)));
        }

        $em->flush();

        return new JsonResponse(['ok' => true], Response::HTTP_OK);
    }

    /**
     * Heartbeat: actualiza el último heartbeat en Status y devuelve comandos pendientes.
     * Body JSON: deviceId (MAC), opcional: batteryLevel, appVersion, appVersionCode.
     * Respuesta: { "commands": [ { "type": "reboot" }, { "type": "update", "apkUrl": "..." } ] }
     *
     * @Rest\Post("/heartbeat", name="api_mdm_heartbeat")
     */
    public function heartbeat(Request $request): Response
    {
        $params = $this->getPostData($request);
        if (empty($params['deviceId'])) {
            return new JsonResponse(['error' => 'deviceId is required'], Response::HTTP_BAD_REQUEST);
        }

        $mac = $this->normalizeMac($params['deviceId']);
        $em = $this->getDoctrine()->getManager();
        $statusRepo = $em->getRepository(Status::class);
        $status = $statusRepo->findOneByMac($mac);

        if (!$status) {
            return new JsonResponse(['error' => 'Device not registered'], Response::HTTP_FORBIDDEN);
        }

        $status->setDate(new \DateTime());
        $currentStatus = $status->getStatus() ?? [];
        $commands = $currentStatus['pendingCommands'] ?? [];
        $newStatus = array_merge($currentStatus, [
            'batteryLevel' => $params['batteryLevel'] ?? null,
            'appVersion' => $params['appVersion'] ?? null,
            'appVersionCode' => $params['appVersionCode'] ?? null,
        ]);
        unset($newStatus['pendingCommands']);
        $status->setStatus($newStatus);

        $em->flush();

        return new JsonResponse(['commands' => $commands], Response::HTTP_OK);
    }

    private function getPostData(Request $request): array
    {
        $content = $request->getContent();
        if (!$content) {
            return [];
        }
        $decoded = json_decode($content, true);
        return is_array($decoded) ? $decoded : [];
    }

    private function normalizeMac(string $deviceId): string
    {
        $mac = preg_replace('/[^0-9a-fA-F]/', '', $deviceId);
        return substr($mac, 0, 12);
    }

    private function buildStatusPayload(array $params): array
    {
        $payload = [];
        if (isset($params['model'])) {
            $payload['model'] = $params['model'];
        }
        if (isset($params['manufacturer'])) {
            $payload['manufacturer'] = $params['manufacturer'];
        }
        if (isset($params['androidVersion'])) {
            $payload['androidVersion'] = $params['androidVersion'];
        }
        if (isset($params['sdkInt'])) {
            $payload['sdkInt'] = $params['sdkInt'];
        }
        if (isset($params['appVersion'])) {
            $payload['appVersion'] = $params['appVersion'];
        }
        if (isset($params['appVersionCode'])) {
            $payload['appVersionCode'] = $params['appVersionCode'];
        }
        return $payload;
    }
}
