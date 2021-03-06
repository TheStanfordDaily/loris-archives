// @flow

import React, { Component } from 'react';
import type { Deployment } from './models'
import './DeploymentList.css';

class DeploymentList extends Component {
  props: {
    deployments: Array<Deployment>
  }

  render() {
    const deployments = this.props.deployments;
    const ongoingDeployment = deployments.length > 1;

    return <div className={
        'DeploymentList ' + (
        ongoingDeployment ? 'DeploymentList__Deploying' : ''
        )}>
      {
          deployments.map(deployment => {

            const taskDefinition = deployment
                .taskDefinition
                .taskDefinitionArn
                .split("/")[1]

            const taskVersion = (taskDefinition || "").split(":")[1]

            const containerDefinitions = deployment
                .taskDefinition
                .containerDefinitions

            const classModifier = (() => { switch(deployment.status) {
              case "ACTIVE":
                return ongoingDeployment ? "Blue" : "Red";
              case "PRIMARY":
                return ongoingDeployment ? "Green" : "Blue";
              case "INACTIVE":
                return "Inactive";
              default:
                return "Red";
            }})()

            const containers = containerDefinitions
                .map(container => {
                    return <div className="Container--Details">
                        <span className="Container--Name">
                            {container.name}
                        </span>
                        <span className="Container--Image">
                            {container.image}
                        </span>
                    </div>
                })

            console.log(containers)

            const deploymentClassName = 'Deployment__' + classModifier;

            return <div className={
                'Deployment ' + deploymentClassName
                }  key={deployment.id}>

              <div className="Deployment--Details">
                  {deployment.status}
                  <a className="Deployment--Status">
                    {taskVersion}
                  </a>
                  <div className="ContainersBox">
                    {containers}
                  </div>
              </div>
            </div>

          })
      }
    </div>
  }
}


export default DeploymentList;
